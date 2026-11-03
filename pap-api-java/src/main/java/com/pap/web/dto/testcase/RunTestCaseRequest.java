package com.pap.web.dto.testcase;

/** Optional body for running a test case; when {@code inputData} is null the stored input is used. */
public record RunTestCaseRequest(String inputData) {
}
