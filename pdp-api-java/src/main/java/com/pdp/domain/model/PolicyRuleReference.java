package com.pdp.domain.model;

import java.util.UUID;

/** Reference to a rule from a policy: either an inline rule or a rule from the shared rule library. */
public record PolicyRuleReference(String type, UUID sharedRuleId, PolicyRule inlineRule) {

    public static final String INLINE = "inline";
    public static final String SHARED = "shared";

    public PolicyRuleReference {
        type = type == null ? INLINE : type;
    }
}
