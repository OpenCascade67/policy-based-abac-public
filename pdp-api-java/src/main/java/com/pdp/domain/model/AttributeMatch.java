package com.pdp.domain.model;

import com.fasterxml.jackson.databind.JsonNode;

/** A single attribute comparison used in rule targets and conditions. {@code value} keeps the raw JSON value. */
public record AttributeMatch(String attributeId, MatchFunction matchFunction, JsonNode value) {

    public AttributeMatch {
        attributeId = attributeId == null ? "" : attributeId;
        matchFunction = matchFunction == null ? MatchFunction.EQUALS : matchFunction;
        value = value == null || value.isNull() ? null : value;
    }
}
