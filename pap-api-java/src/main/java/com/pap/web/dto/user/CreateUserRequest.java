package com.pap.web.dto.user;

import com.pap.web.dto.DomainRoleDto;

import java.util.List;

/** Body for creating a local (username/password) user. {@code status} is "active" or "inactive". */
public record CreateUserRequest(String username, String password, String status, List<DomainRoleDto> domainRoles) {

    public CreateUserRequest {
        username = username == null ? "" : username;
        password = password == null ? "" : password;
        status = status == null ? "active" : status;
        domainRoles = domainRoles == null ? List.of() : domainRoles;
    }
}
