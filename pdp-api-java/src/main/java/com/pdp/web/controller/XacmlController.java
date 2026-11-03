package com.pdp.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pdp.security.CurrentClient;
import com.pdp.xacml.XacmlDecisionService;
import com.pdp.xacml.XacmlModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Decisions in the XACML 3.0 JSON Profile format ({@code application/xacml+json}). */
@Tag(name = "XACML")
@RestController
@RequestMapping("/api/v1/domains/{domainId}/xacml")
public class XacmlController {

    public static final String XACML_JSON = "application/xacml+json";

    private final XacmlDecisionService xacmlDecisionService;
    private final CurrentClient currentClient;

    public XacmlController(XacmlDecisionService xacmlDecisionService, CurrentClient currentClient) {
        this.xacmlDecisionService = xacmlDecisionService;
        this.currentClient = currentClient;
    }

    @Operation(summary = "Decide a XACML 3.0 JSON Profile request",
            description = "Supports shorthand and Category-array categories, ReturnPolicyIdList, IncludeInResult, and "
                    + "multiple decisions via repeated categories or MultiRequests. CombinedDecision is not supported.")
    @PostMapping(path = "/decisions",
            consumes = {XACML_JSON, MediaType.APPLICATION_JSON_VALUE},
            produces = {XACML_JSON, MediaType.APPLICATION_JSON_VALUE})
    public XacmlModel.Response decide(@PathVariable UUID domainId, @RequestBody JsonNode request) {
        return xacmlDecisionService.decide(currentClient.get(), domainId, request);
    }
}
