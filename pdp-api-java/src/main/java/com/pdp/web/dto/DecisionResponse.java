package com.pdp.web.dto;

import com.pdp.domain.model.Obligation;
import com.pdp.domain.model.Policy;
import com.pdp.evaluation.Decision;
import com.pdp.evaluation.EvaluationResult;

import java.util.List;

/**
 * A decision in the PAP's test-case format.
 *
 * @param decision         "Permit", "Deny", "NotApplicable" or "Indeterminate"
 * @param matchedPolicies  names of the policies that produced the decision
 * @param obligations      obligations the PEP must fulfil along with the decision
 */
public record DecisionResponse(Decision decision, List<String> matchedPolicies, List<Obligation> obligations) {

    public static DecisionResponse from(EvaluationResult result) {
        return new DecisionResponse(
                result.decision(),
                result.matchedPolicies().stream().map(Policy::name).toList(),
                result.obligations());
    }
}
