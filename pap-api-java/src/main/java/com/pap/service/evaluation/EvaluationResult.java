package com.pap.service.evaluation;

import com.pap.domain.model.Obligation;

import java.util.List;

/**
 * @param decision                "Permit", "Deny", "NotApplicable" or "Indeterminate"
 * @param matchedPolicies         names of the policies that produced {@code decision}
 * @param applicableObligations   obligations of those policies that apply to {@code decision}
 */
public record EvaluationResult(String decision, List<String> matchedPolicies, List<Obligation> applicableObligations) {

    static EvaluationResult indeterminate() {
        return new EvaluationResult("Indeterminate", List.of(), List.of());
    }
}
