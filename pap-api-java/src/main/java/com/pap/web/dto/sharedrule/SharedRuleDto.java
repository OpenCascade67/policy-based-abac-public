package com.pap.web.dto.sharedrule;

import com.pap.domain.model.PolicyCondition;
import com.pap.domain.model.PolicyTarget;
import com.pap.domain.model.SharedRule;
import com.pap.json.EnumNames;
import com.pap.web.dto.Ids;

import java.time.Instant;
import java.util.List;

public record SharedRuleDto(
        String id,
        String name,
        String description,
        String category,
        String effect,
        PolicyTarget target,
        PolicyCondition condition,
        List<String> usedByPolicies,
        String createdById,
        String lastModifiedById,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static SharedRuleDto from(SharedRule rule) {
        return new SharedRuleDto(
                Ids.format(rule.getId()),
                rule.getName(),
                rule.getDescription(),
                rule.getCategory(),
                EnumNames.pascal(rule.getEffect()),
                rule.getTarget(),
                rule.getCondition(),
                rule.getUsedByPolicies().stream().map(Ids::format).toList(),
                Ids.format(rule.getCreatedById()),
                Ids.format(rule.getLastModifiedById()),
                rule.getCreatedAt(),
                rule.getLastModifiedAt());
    }
}
