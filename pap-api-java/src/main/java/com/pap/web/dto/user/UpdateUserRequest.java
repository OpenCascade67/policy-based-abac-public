package com.pap.web.dto.user;

import com.pap.web.dto.DomainRoleDto;

import java.util.List;

/** Body for updating a user; an empty {@code password} leaves the password unchanged. */
public record UpdateUserRequest(String password, String status, List<DomainRoleDto> domainRoles) {

    public UpdateUserRequest {
        status = status == null ? "active" : status;
        domainRoles = domainRoles == null ? List.of() : domainRoles;
    }
}
