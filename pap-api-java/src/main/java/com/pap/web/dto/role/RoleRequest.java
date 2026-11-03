package com.pap.web.dto.role;

/** Body for creating or updating a custom role. */
public record RoleRequest(String name, String description) {

    public RoleRequest {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
    }
}
