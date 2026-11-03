package com.pdp.domain.model;

public record PolicyRule(
        String ruleId,
        RuleEffect effect,
        String description,
        PolicyTarget target,
        PolicyCondition condition) {

    public PolicyRule {
        ruleId = ruleId == null ? "" : ruleId;
        effect = effect == null ? RuleEffect.PERMIT : effect;
    }
}
