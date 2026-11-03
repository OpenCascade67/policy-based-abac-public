package com.pdp.domain.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ObligationAttributeAssignment(String attributeId, JsonNode value, AttributeDataType dataType) {

    public ObligationAttributeAssignment {
        attributeId = attributeId == null ? "" : attributeId;
        value = value == null || value.isNull() ? null : value;
    }
}
