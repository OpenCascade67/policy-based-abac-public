package com.pdp.domain.model;

import java.util.UUID;

/** A reusable rule from the PAP's shared rule library ({@code shared-rules.json}). */
public record SharedRule(
        UUID id,
        UUID domainId,
        String name,
        String description,
        RuleEffect effect,
        PolicyTarget target,
        PolicyCondition condition) {

    public SharedRule {
        effect = effect == null ? RuleEffect.PERMIT : effect;
    }

    public UUID effectiveDomainId() {
        return Domains.effective(domainId);
    }

    /** Views this shared rule as a policy rule so it is evaluated exactly like an inline one. */
    public PolicyRule toPolicyRule() {
        return new PolicyRule(String.valueOf(id), effect, description, target, condition);
    }
}
