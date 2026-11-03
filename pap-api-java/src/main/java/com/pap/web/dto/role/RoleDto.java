package com.pap.web.dto.role;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pap.domain.model.Role;
import com.pap.web.dto.Ids;

import java.time.Instant;
import java.util.List;

public record RoleDto(
        String id,
        String name,
        String description,
        @JsonProperty("isSystemRole") boolean isSystemRole,
        List<String> permissions,
        String createdById,
        String lastModifiedById,
        Instant createdAt,
        Instant lastModifiedAt) {

    public static RoleDto from(Role role) {
        return new RoleDto(
                Ids.format(role.getId()),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                List.copyOf(role.getPermissions()),
                Ids.format(role.getCreatedById()),
                Ids.format(role.getLastModifiedById()),
                role.getCreatedAt(),
                role.getLastModifiedAt());
    }
}
