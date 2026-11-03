package com.pdp.web.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Several independent requests, each in the {@code {subject, resource, action, environment}} form. */
public record BatchDecisionRequest(@NotNull List<JsonNode> requests) {
}
