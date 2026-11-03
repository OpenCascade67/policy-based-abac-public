package com.pdp.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pdp.common.exception.BadRequestException;
import com.pdp.security.CurrentClient;
import com.pdp.service.DecisionService;
import com.pdp.web.dto.BatchDecisionRequest;
import com.pdp.web.dto.BatchDecisionResponse;
import com.pdp.web.dto.DecisionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/** Decisions in the PAP's native request format, the same JSON the PAP's test cases use. */
@Tag(name = "Decisions")
@RestController
@RequestMapping(path = "/api/v1/domains/{domainId}/decisions",
        consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class DecisionController {

    private final DecisionService decisionService;
    private final CurrentClient currentClient;

    public DecisionController(DecisionService decisionService, CurrentClient currentClient) {
        this.decisionService = decisionService;
        this.currentClient = currentClient;
    }

    @Operation(summary = "Decide one request",
            description = "Body: {\"subject\": {...}, \"resource\": {...}, \"action\": {...}, \"environment\": {...}}")
    @PostMapping
    public DecisionResponse decide(
            @PathVariable UUID domainId,
            @Parameter(description = "Evaluate only this policy instead of all policies of the domain")
            @RequestParam(required = false) UUID policyId,
            @RequestBody JsonNode request) {
        requireObject(request, "The request body");
        var scope = decisionService.scope(currentClient.get(), domainId, policyId);
        return DecisionResponse.from(decisionService.evaluate(scope, request));
    }

    @Operation(summary = "Decide several independent requests against the same policy snapshot")
    @PostMapping("/batch")
    public BatchDecisionResponse decideBatch(
            @PathVariable UUID domainId,
            @Parameter(description = "Evaluate only this policy instead of all policies of the domain")
            @RequestParam(required = false) UUID policyId,
            @Valid @RequestBody BatchDecisionRequest batch) {
        List<JsonNode> requests = batch.requests();
        IntStream.range(0, requests.size()).forEach(i -> requireObject(requests.get(i), "requests[" + i + "]"));
        var scope = decisionService.scope(currentClient.get(), domainId, policyId);
        return new BatchDecisionResponse(decisionService.evaluate(scope, requests).stream().map(DecisionResponse::from).toList());
    }

    private static void requireObject(JsonNode node, String what) {
        if (node == null || !node.isObject()) {
            throw new BadRequestException(what + " must be a JSON object with subject, resource, action and environment.");
        }
    }
}
