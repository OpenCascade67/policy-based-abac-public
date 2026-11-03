package com.pdp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pdp.common.exception.BadRequestException;
import com.pdp.common.exception.ForbiddenException;
import com.pdp.common.exception.NotFoundException;
import com.pdp.config.PdpProperties;
import com.pdp.domain.model.Domain;
import com.pdp.domain.model.Policy;
import com.pdp.domain.model.SharedRule;
import com.pdp.evaluation.EvaluationResult;
import com.pdp.evaluation.PolicyEvaluator;
import com.pdp.security.ApiClient;
import com.pdp.store.PolicySnapshot;
import com.pdp.store.PolicyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Makes authorization decisions for one administration domain. All requests in a call are evaluated against
 * the same policy snapshot, so a batch never mixes old and new policies.
 */
@Service
public class DecisionService {

    private static final Logger audit = LoggerFactory.getLogger("com.pdp.audit");

    /**
     * The policies a client may decide against: one domain (or a single policy in it) from one snapshot.
     *
     * @param policyId the single policy selected, or {@code null} for all policies of the domain
     */
    public record DecisionScope(ApiClient client, UUID domainId, UUID policyId,
                                List<Policy> policies, List<SharedRule> sharedRules) {
    }

    private final PolicyStore store;
    private final PolicyEvaluator evaluator;
    private final int maxBatchSize;

    public DecisionService(PolicyStore store, PolicyEvaluator evaluator, PdpProperties properties) {
        this.store = store;
        this.evaluator = evaluator;
        this.maxBatchSize = properties.maxBatchSize();
    }

    /**
     * Checks the client may use the domain and that it exists and is active.
     *
     * @param policyId restrict decisions to this policy, or {@code null} for all policies of the domain
     * @throws ForbiddenException if the client is not allowed in the domain or the domain is inactive
     * @throws NotFoundException  if the domain or policy does not exist
     */
    public DecisionScope scope(ApiClient client, UUID domainId, UUID policyId) {
        if (!client.mayAccess(domainId)) {
            throw new ForbiddenException("Client '%s' may not request decisions for domain '%s'.".formatted(client.name(), domainId));
        }
        PolicySnapshot snapshot = store.current();
        Domain domain = snapshot.domain(domainId).orElseThrow(() -> new NotFoundException("Domain", domainId));
        if (!domain.isActive()) throw new ForbiddenException("Domain '%s' is inactive.".formatted(domainId));

        List<Policy> inDomain = snapshot.policiesIn(domainId);
        List<Policy> policies = policyId == null
                ? inDomain
                : List.of(inDomain.stream()
                        .filter(p -> policyId.equals(p.id()))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException("Policy", policyId)));
        return new DecisionScope(client, domainId, policyId, policies, snapshot.sharedRulesIn(domainId));
    }

    /** Evaluates each request independently; results are in request order. */
    public List<EvaluationResult> evaluate(DecisionScope scope, List<JsonNode> requests) {
        if (requests.size() > maxBatchSize) {
            throw new BadRequestException("A request may contain at most %d decisions; %d were requested."
                    .formatted(maxBatchSize, requests.size()));
        }
        return requests.stream().map(request -> evaluate(scope, request)).toList();
    }

    public EvaluationResult evaluate(DecisionScope scope, JsonNode request) {
        long started = System.nanoTime();
        EvaluationResult result = evaluator.evaluate(scope.policies(), scope.sharedRules(), request);
        audit.info("client={} domain={} policy={} decision={} matched={} micros={}",
                scope.client().name(), scope.domainId(), scope.policyId() == null ? "*" : scope.policyId(),
                result.decision(), result.matchedPolicies().stream().map(Policy::name).toList(),
                (System.nanoTime() - started) / 1_000);
        return result;
    }
}
