package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A reusable rule that policies can reference from the shared rule library. */
@JsonPropertyOrder({"id", "domainId", "name", "description", "category", "effect", "target", "condition", "usedByPolicies",
        "createdAt", "createdById", "lastModifiedAt", "lastModifiedById"})
public class SharedRule extends AuditableEntity implements DomainScoped {

    private UUID domainId = UNASSIGNED;
    private String name = "";
    private String description = "";
    private String category = "";
    private RuleEffect effect = RuleEffect.PERMIT;
    private PolicyTarget target;
    private PolicyCondition condition;
    private List<UUID> usedByPolicies = new ArrayList<>();

    @Override public UUID getDomainId() { return domainId; }
    @Override public void setDomainId(UUID domainId) { this.domainId = domainId == null ? UNASSIGNED : domainId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public RuleEffect getEffect() { return effect; }
    public void setEffect(RuleEffect effect) { this.effect = effect; }

    public PolicyTarget getTarget() { return target; }
    public void setTarget(PolicyTarget target) { this.target = target; }

    public PolicyCondition getCondition() { return condition; }
    public void setCondition(PolicyCondition condition) { this.condition = condition; }

    public List<UUID> getUsedByPolicies() { return usedByPolicies; }
    public void setUsedByPolicies(List<UUID> usedByPolicies) {
        this.usedByPolicies = usedByPolicies == null ? new ArrayList<>() : new ArrayList<>(usedByPolicies);
    }

    /** Views this shared rule as a policy rule so it is evaluated exactly like an inline one. */
    public PolicyRule toPolicyRule() {
        return new PolicyRule(String.valueOf(getId()), effect, description, target, condition);
    }
}
