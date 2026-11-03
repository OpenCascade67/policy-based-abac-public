package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A named ABAC request (stored as a JSON string) that test cases can start from. */
@JsonPropertyOrder({"id", "domainId", "name", "description", "value", "testCases",
        "createdAt", "createdById", "lastModifiedAt", "lastModifiedById"})
public class Scenario extends AuditableEntity implements DomainScoped {

    private UUID domainId = UNASSIGNED;
    private String name = "";
    private String description = "";
    private String value = "{}";
    private List<UUID> testCases = new ArrayList<>();

    @Override public UUID getDomainId() { return domainId; }
    @Override public void setDomainId(UUID domainId) { this.domainId = domainId == null ? UNASSIGNED : domainId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public List<UUID> getTestCases() { return testCases; }
    public void setTestCases(List<UUID> testCases) {
        this.testCases = testCases == null ? new ArrayList<>() : new ArrayList<>(testCases);
    }
}
