package com.pap.web.dto.testcase;

import com.pap.domain.model.TestDecision;
import com.pap.domain.model.TestResult;
import com.pap.json.EnumNames;

import java.time.Instant;
import java.util.List;

/** {@code decision} is one of "Permit", "Deny", "NotApplicable", "Indeterminate". */
public record TestResultDto(String decision, List<String> matchedPolicies, Instant executedAt) {

    public TestResultDto {
        decision = decision == null ? "" : decision;
        matchedPolicies = matchedPolicies == null ? List.of() : matchedPolicies;
    }

    public static TestResultDto from(TestResult result) {
        return new TestResultDto(EnumNames.pascal(result.decision()), result.matchedPolicies(), result.executedAt());
    }

    /** @throws IllegalArgumentException if {@code decision} is not an exact decision name */
    public TestResult toTestResult() {
        return new TestResult(EnumNames.parseExact(TestDecision.class, decision), matchedPolicies, executedAt);
    }
}
