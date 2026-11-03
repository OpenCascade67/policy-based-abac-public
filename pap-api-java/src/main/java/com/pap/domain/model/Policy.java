package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.UUID;

@JsonPropertyOrder({"id", "domainId", "name", "description", "policyDefinition",
        "createdAt", "createdById", "lastModifiedAt", "lastModifiedById"})
public class Policy extends AuditableEntity implements DomainScoped {

    private UUID domainId = UNASSIGNED;
    private String name = "";
    private String description = "";
    private PolicyDefinition policyDefinition = new PolicyDefinition(null, null, null, null, null, null);

    @Override public UUID getDomainId() { return domainId; }
    @Override public void setDomainId(UUID domainId) { this.domainId = domainId == null ? UNASSIGNED : domainId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PolicyDefinition getPolicyDefinition() { return policyDefinition; }
    public void setPolicyDefinition(PolicyDefinition policyDefinition) { this.policyDefinition = policyDefinition; }
}
