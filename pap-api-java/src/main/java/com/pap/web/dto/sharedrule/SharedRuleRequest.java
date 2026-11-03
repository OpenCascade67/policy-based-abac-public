package com.pap.web.dto.sharedrule;

import com.pap.domain.model.PolicyCondition;
import com.pap.domain.model.PolicyTarget;

/** Body for creating or updating a shared rule. {@code effect} is "Permit" or "Deny". */
public record SharedRuleRequest(
        String name,
        String description,
        String category,
        String effect,
        PolicyTarget target,
        PolicyCondition condition) {

    public SharedRuleRequest {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        category = category == null ? "" : category;
        effect = effect == null ? "Permit" : effect;
    }
}
