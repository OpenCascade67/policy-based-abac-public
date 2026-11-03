package com.pdp.xacml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.pdp.config.PdpProperties;
import com.pdp.domain.model.ObligationAttributeAssignment;
import com.pdp.domain.model.Policy;
import com.pdp.evaluation.Decision;
import com.pdp.evaluation.EvaluationResult;
import com.pdp.security.ApiClient;
import com.pdp.service.DecisionService;
import com.pdp.xacml.XacmlModel.AttributeAssignment;
import com.pdp.xacml.XacmlModel.IdReference;
import com.pdp.xacml.XacmlModel.IndividualRequest;
import com.pdp.xacml.XacmlModel.Obligation;
import com.pdp.xacml.XacmlModel.ParsedRequest;
import com.pdp.xacml.XacmlModel.PolicyIdentifierList;
import com.pdp.xacml.XacmlModel.Response;
import com.pdp.xacml.XacmlModel.Result;
import com.pdp.xacml.XacmlModel.Status;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** Answers XACML 3.0 JSON Profile requests using the same decision engine as the native API. */
@Service
public class XacmlDecisionService {

    private final XacmlRequestParser parser;
    private final DecisionService decisionService;
    private final int maxBatchSize;

    public XacmlDecisionService(XacmlRequestParser parser, DecisionService decisionService, PdpProperties properties) {
        this.parser = parser;
        this.decisionService = decisionService;
        this.maxBatchSize = properties.maxBatchSize();
    }

    /**
     * Malformed XACML yields a single Indeterminate result with a syntax-error status, as the profile requires;
     * access and domain problems are HTTP errors raised before the body is looked at.
     */
    public Response decide(ApiClient client, UUID domainId, JsonNode body) {
        DecisionService.DecisionScope scope = decisionService.scope(client, domainId, null);
        ParsedRequest request;
        try {
            request = parser.parse(body, maxBatchSize);
        } catch (XacmlSyntaxException e) {
            return single(indeterminate(Status.SYNTAX_ERROR, e.getMessage()));
        }
        if (request.combinedDecision()) {
            return single(indeterminate(Status.PROCESSING_ERROR, "CombinedDecision is not supported by this PDP."));
        }

        List<IndividualRequest> individual = request.individualRequests();
        List<EvaluationResult> evaluations = decisionService.evaluate(
                scope, individual.stream().map(IndividualRequest::input).toList());

        List<Result> results = IntStream.range(0, individual.size())
                .mapToObj(i -> toResult(evaluations.get(i), individual.get(i), request.returnPolicyIdList()))
                .toList();
        return new Response(results);
    }

    private static Result toResult(EvaluationResult evaluation, IndividualRequest request, boolean returnPolicyIdList) {
        Status status = evaluation.decision() == Decision.INDETERMINATE
                ? Status.of(Status.PROCESSING_ERROR, "One or more policies could not be evaluated.")
                : Status.of(Status.OK, null);
        PolicyIdentifierList policyIds = returnPolicyIdList
                ? new PolicyIdentifierList(evaluation.matchedPolicies().stream().map(XacmlDecisionService::idReference).toList())
                : null;
        return new Result(evaluation.decision(), status,
                evaluation.obligations().stream().map(XacmlDecisionService::obligation).toList(),
                request.includedInResult(), policyIds);
    }

    private static IdReference idReference(Policy policy) {
        String id = policy.policyDefinition().policyId();
        return new IdReference(id == null || id.isEmpty() ? String.valueOf(policy.id()) : id, policy.policyDefinition().version());
    }

    private static Obligation obligation(com.pdp.domain.model.Obligation obligation) {
        List<AttributeAssignment> assignments = new ArrayList<>();
        for (ObligationAttributeAssignment assignment : obligation.attributeAssignments()) {
            JsonNode value = assignment.value() == null ? JsonNodeFactory.instance.nullNode() : assignment.value();
            String dataType = assignment.dataType() == null ? null : assignment.dataType().uri();
            assignments.add(new AttributeAssignment(assignment.attributeId(), value, dataType));
        }
        return new Obligation(obligation.obligationId(), assignments);
    }

    private static Result indeterminate(String statusCode, String message) {
        return new Result(Decision.INDETERMINATE, Status.of(statusCode, message), List.of(), List.of(), null);
    }

    private static Response single(Result result) {
        return new Response(List.of(result));
    }
}
