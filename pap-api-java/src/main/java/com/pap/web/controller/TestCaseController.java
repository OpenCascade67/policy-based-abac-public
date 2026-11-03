package com.pap.web.controller;

import com.pap.service.TestCaseService;
import com.pap.web.dto.PagedResult;
import com.pap.web.dto.testcase.RunTestCaseRequest;
import com.pap.web.dto.testcase.RunTestCaseResponse;
import com.pap.web.dto.testcase.TestCaseDto;
import com.pap.web.dto.testcase.TestCaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Test Cases")
@RestController
@RequestMapping("/api/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @GetMapping
    public PagedResult<TestCaseDto> list(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        return testCaseService.list(page, pageSize);
    }

    /** Responds 200 (not 201), as the UI expects. */
    @PostMapping
    public TestCaseDto create(@RequestBody TestCaseRequest request) {
        return testCaseService.create(request);
    }

    @PutMapping("/{id}")
    public TestCaseDto update(@PathVariable UUID id, @RequestBody TestCaseRequest request) {
        return testCaseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        testCaseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Evaluate the test case and record the result",
            description = "The body is optional; inputData overrides the stored input for this run.")
    @PostMapping("/{id}/run")
    public RunTestCaseResponse run(@PathVariable UUID id, @RequestBody(required = false) RunTestCaseRequest request) {
        return testCaseService.run(id, request == null ? null : request.inputData());
    }
}
