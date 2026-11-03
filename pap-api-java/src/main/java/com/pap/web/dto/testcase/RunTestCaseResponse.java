package com.pap.web.dto.testcase;

import java.time.Instant;
import java.util.List;

public record RunTestCaseResponse(String decision, List<String> matchedPolicies, Instant executedAt) {
}
