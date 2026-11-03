package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({"id", "name", "description", "isSystemRole", "permissions",
        "createdAt", "createdById", "lastModifiedAt", "lastModifiedById"})
public class Role extends AuditableEntity {

    private String name = "";
    private String description = "";
    private boolean systemRole;
    private List<String> permissions = new ArrayList<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @JsonProperty("isSystemRole")
    public boolean isSystemRole() { return systemRole; }

    @JsonProperty("isSystemRole")
    public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }

    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
    }
}
