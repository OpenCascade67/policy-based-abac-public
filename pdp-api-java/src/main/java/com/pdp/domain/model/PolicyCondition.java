package com.pdp.domain.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * A boolean combination of conditions. Each entry is either a nested {@code PolicyCondition}
 * or an {@link AttributeMatch}; entries are kept as raw JSON and interpreted during evaluation.
 */
public record PolicyCondition(String operator, List<JsonNode> conditions) {

    public PolicyCondition {
        operator = operator == null ? "AND" : operator;
        conditions = conditions == null ? List.of() : conditions;
    }
}
