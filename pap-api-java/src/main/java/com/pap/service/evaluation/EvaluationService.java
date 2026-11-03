package com.pap.service.evaluation;

import com.pap.domain.model.Policy;
import com.pap.repository.PolicyRepository;
import com.pap.repository.SharedRuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Evaluates a JSON request against one policy, or against all policies of the current domain. */
@Service
public class EvaluationService {

    private final PolicyRepository policies;
    private final SharedRuleRepository sharedRules;
    private final PolicyEvaluator evaluator;

    public EvaluationService(PolicyRepository policies, SharedRuleRepository sharedRules, PolicyEvaluator evaluator) {
        this.policies = policies;
        this.sharedRules = sharedRules;
        this.evaluator = evaluator;
    }

    /** @param policyId the policy to test, or {@code null} for all policies */
    public EvaluationResult evaluate(UUID policyId, String inputData) {
        List<Policy> selected = policyId == null
                ? policies.findAll()
                : policies.findById(policyId).stream().toList();
        return evaluator.evaluate(selected, sharedRules.findAll(), inputData);
    }
}
