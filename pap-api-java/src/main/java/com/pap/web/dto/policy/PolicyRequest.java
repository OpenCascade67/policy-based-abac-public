package com.pap.web.dto.policy;

/** Body for creating or updating a policy. */
public record PolicyRequest(String name, String description, PolicyDefinitionDto policyDefinition) {

    public PolicyRequest {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        policyDefinition = policyDefinition == null ? PolicyDefinitionDto.empty() : policyDefinition;
    }
}
