package com.pap.web.dto.policy;

import com.pap.domain.model.Policy;
import com.pap.web.dto.Ids;

import java.time.Instant;

public record PolicyDto(
        String id,
        String name,
        String description,
        PolicyDefinitionDto policyDefinition,
        String createdById,
        String lastModifiedById,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static PolicyDto from(Policy policy) {
        return new PolicyDto(
                Ids.format(policy.getId()),
                policy.getName(),
                policy.getDescription(),
                PolicyDefinitionDto.from(policy.getPolicyDefinition()),
                Ids.format(policy.getCreatedById()),
                Ids.format(policy.getLastModifiedById()),
                policy.getCreatedAt(),
                policy.getLastModifiedAt());
    }
}
