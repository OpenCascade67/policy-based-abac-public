package com.pap.domain.model;

import java.time.Instant;
import java.util.List;

public record TestResult(TestDecision decision, List<String> matchedPolicies, Instant executedAt) {

    public TestResult {
        decision = decision == null ? TestDecision.PERMIT : decision;
        matchedPolicies = matchedPolicies == null ? List.of() : matchedPolicies;
    }
}
