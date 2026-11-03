package com.pap.web.dto.domain;

/** Body for creating or updating a domain. {@code status} is "active" or "inactive". */
public record DomainRequest(String name, String description, String status) {

    public DomainRequest {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        status = status == null ? "active" : status;
    }
}
