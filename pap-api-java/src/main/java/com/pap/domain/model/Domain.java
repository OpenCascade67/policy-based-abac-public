package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.UUID;

/** An administration domain that scopes policies, shared rules, scenarios and test cases. */
@JsonPropertyOrder({"id", "name", "description", "status", "createdAt", "createdById", "lastModifiedAt", "lastModifiedById"})
public class Domain extends AuditableEntity {

    /** Well-known id of the domain seeded at first start-up; unassigned legacy data belongs to it. */
    public static final UUID DEFAULT_DOMAIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private String name = "";
    private String description = "";
    private DomainStatus status = DomainStatus.ACTIVE;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public DomainStatus getStatus() { return status; }
    public void setStatus(DomainStatus status) { this.status = status; }
}
