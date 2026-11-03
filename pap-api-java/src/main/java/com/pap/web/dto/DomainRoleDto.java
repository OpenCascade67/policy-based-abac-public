package com.pap.web.dto;

import com.pap.domain.model.DomainRole;

import java.util.List;

/** Roles held in one domain; ids are strings on the wire. */
public record DomainRoleDto(String domainId, List<String> roles) {

    public DomainRoleDto {
        domainId = domainId == null ? "" : domainId;
        roles = roles == null ? List.of() : roles;
    }

    public static DomainRoleDto from(DomainRole role) {
        return new DomainRoleDto(Ids.format(role.domainId()), role.roles().stream().map(Ids::format).toList());
    }

    /** @throws IllegalArgumentException if an id is not a GUID */
    public DomainRole toDomainRole() {
        return new DomainRole(Ids.parse(domainId), roles.stream().map(Ids::parse).toList());
    }
}
