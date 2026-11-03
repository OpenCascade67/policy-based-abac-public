package com.pap.service;

import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.domain.model.TestCase;
import com.pap.domain.model.TestDecision;
import com.pap.domain.model.TestResult;
import com.pap.json.EnumNames;
import com.pap.repository.TestCaseRepository;
import com.pap.security.CurrentUser;
import com.pap.service.evaluation.EvaluationResult;
import com.pap.service.evaluation.EvaluationService;
import com.pap.web.dto.Ids;
import com.pap.web.dto.PagedResult;
import com.pap.web.dto.testcase.RunTestCaseResponse;
import com.pap.web.dto.testcase.TestCaseDto;
import com.pap.web.dto.testcase.TestCaseRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class TestCaseService {

    private static final String TEST_CASE = "TestCase";

    private final TestCaseRepository testCases;
    private final EvaluationService evaluationService;
    private final CurrentUser currentUser;
    private final Clock clock;

    public TestCaseService(TestCaseRepository testCases, EvaluationService evaluationService,
                           CurrentUser currentUser, Clock clock) {
        this.testCases = testCases;
        this.evaluationService = evaluationService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    public PagedResult<TestCaseDto> list(int page, int pageSize) {
        return PagedResult.of(testCases.findAll(), page, pageSize, TestCaseDto::from);
    }

    public TestCaseDto create(TestCaseRequest request) {
        Validator.create()
                .notEmpty("Request.Name", request.name())
                .maxLength("Request.Name", request.name(), 200)
                .validate();

        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID());
        apply(testCase, request);
        testCase.markCreated(clock.instant(), currentUser.userIdOrNull());
        return TestCaseDto.from(testCases.add(testCase));
    }

    public TestCaseDto update(UUID id, TestCaseRequest request) {
        TestCase testCase = find(id);
        apply(testCase, request);
        if (request.result() != null) testCase.setResult(request.result().toTestResult());
        testCase.markModified(clock.instant(), currentUser.userIdOrNull());
        return TestCaseDto.from(testCases.update(testCase));
    }

    public void delete(UUID id) {
        if (!testCases.existsById(id)) throw new NotFoundException(TEST_CASE, id);
        testCases.deleteById(id);
    }

    /**
     * Evaluates the test case and stores the outcome as its latest result.
     *
     * @param inputData request JSON to evaluate; {@code null} uses the stored input
     */
    public RunTestCaseResponse run(UUID id, String inputData) {
        TestCase testCase = find(id);
        String input = inputData != null ? inputData : testCase.getInputData();
        EvaluationResult evaluation = evaluationService.evaluate(testCase.getPolicyId(), input);

        Instant executedAt = clock.instant();
        TestDecision decision = EnumNames.parseExact(TestDecision.class, evaluation.decision());
        TestResult result = new TestResult(decision, evaluation.matchedPolicies(), executedAt);
        testCase.setResult(result);
        testCase.setLastModifiedAt(executedAt);
        testCases.update(testCase);
        return new RunTestCaseResponse(evaluation.decision(), evaluation.matchedPolicies(), executedAt);
    }

    private TestCase find(UUID id) {
        return testCases.findById(id).orElseThrow(() -> new NotFoundException(TEST_CASE, id));
    }

    private static void apply(TestCase testCase, TestCaseRequest request) {
        testCase.setName(request.name());
        testCase.setDescription(request.description());
        testCase.setPolicyId(Ids.parseOptional(request.policyId()));
        testCase.setScenarioId(Ids.parseOptional(request.scenarioId()));
        testCase.setInputData(request.inputData());
    }
}
