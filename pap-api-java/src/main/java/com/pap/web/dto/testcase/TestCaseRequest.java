package com.pap.web.dto.testcase;

/**
 * Body for creating or updating a test case. An empty or missing {@code policyId} means "all policies";
 * {@code result} is only honoured on update.
 */
public record TestCaseRequest(
        String name,
        String description,
        String policyId,
        String scenarioId,
        String inputData,
        TestResultDto result) {

    public TestCaseRequest {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        inputData = inputData == null ? "{}" : inputData;
    }
}
