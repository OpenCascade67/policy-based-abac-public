package com.pap.domain.model;

import java.util.List;
import java.util.UUID;

/** The roles a user holds within one administration domain. */
public record DomainRole(UUID domainId, List<UUID> roles) {

    public DomainRole {
        roles = roles == null ? List.of() : roles;
    }
}
