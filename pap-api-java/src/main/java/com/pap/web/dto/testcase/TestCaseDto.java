package com.pap.web.dto.testcase;

import com.pap.domain.model.TestCase;
import com.pap.web.dto.Ids;

import java.time.Instant;

public record TestCaseDto(
        String id,
        String name,
        String description,
        String policyId,
        String scenarioId,
        String inputData,
        TestResultDto result,
        String createdById,
        String lastModifiedById,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static TestCaseDto from(TestCase testCase) {
        return new TestCaseDto(
                Ids.format(testCase.getId()),
                testCase.getName(),
                testCase.getDescription(),
                Ids.format(testCase.getPolicyId()),
                Ids.format(testCase.getScenarioId()),
                testCase.getInputData(),
                testCase.getResult() == null ? null : TestResultDto.from(testCase.getResult()),
                Ids.format(testCase.getCreatedById()),
                Ids.format(testCase.getLastModifiedById()),
                testCase.getCreatedAt(),
                testCase.getLastModifiedAt());
    }
}
