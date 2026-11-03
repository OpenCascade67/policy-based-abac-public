package com.pdp.evaluation;

import com.pdp.domain.model.Obligation;
import com.pdp.domain.model.Policy;

import java.util.List;

/**
 * @param decision          the combined decision
 * @param matchedPolicies   the policies that produced {@code decision}
 * @param obligations       obligations of those policies that apply to {@code decision}
 */
public record EvaluationResult(Decision decision, List<Policy> matchedPolicies, List<Obligation> obligations) {

    static EvaluationResult indeterminate() {
        return new EvaluationResult(Decision.INDETERMINATE, List.of(), List.of());
    }
}
