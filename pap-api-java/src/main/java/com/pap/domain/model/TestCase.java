package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.UUID;

@JsonPropertyOrder({"id", "domainId", "name", "description", "policyId", "scenarioId", "inputData", "result",
        "createdAt", "createdById", "lastModifiedAt", "lastModifiedById"})
public class TestCase extends AuditableEntity implements DomainScoped {

    private UUID domainId = UNASSIGNED;
    private String name = "";
    private String description = "";
    private UUID policyId;
    private UUID scenarioId;
    private String inputData = "{}";
    private TestResult result;

    @Override public UUID getDomainId() { return domainId; }
    @Override public void setDomainId(UUID domainId) { this.domainId = domainId == null ? UNASSIGNED : domainId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }

    public UUID getScenarioId() { return scenarioId; }
    public void setScenarioId(UUID scenarioId) { this.scenarioId = scenarioId; }

    public String getInputData() { return inputData; }
    public void setInputData(String inputData) { this.inputData = inputData; }

    public TestResult getResult() { return result; }
    public void setResult(TestResult result) { this.result = result; }
}
