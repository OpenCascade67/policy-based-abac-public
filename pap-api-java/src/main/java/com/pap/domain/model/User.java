package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JsonPropertyOrder({"id", "username", "passwordHash", "email", "userType", "status", "domainRoles", "defaultDomainId",
        "createdAt", "createdById", "lastModifiedAt", "lastModifiedById"})
public class User extends AuditableEntity {

    private String username = "";
    private String passwordHash;
    private String email;
    private UserType userType = UserType.LOCAL;
    private UserStatus status = UserStatus.ACTIVE;
    private List<DomainRole> domainRoles = new ArrayList<>();
    private UUID defaultDomainId;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public List<DomainRole> getDomainRoles() { return domainRoles; }
    public void setDomainRoles(List<DomainRole> domainRoles) {
        this.domainRoles = domainRoles == null ? new ArrayList<>() : new ArrayList<>(domainRoles);
    }

    public UUID getDefaultDomainId() { return defaultDomainId; }
    public void setDefaultDomainId(UUID defaultDomainId) { this.defaultDomainId = defaultDomainId; }

    @JsonIgnore
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isAssignedTo(UUID domainId) {
        return domainRoles.stream().anyMatch(dr -> domainId.equals(dr.domainId()));
    }

    /** The domain a new session starts in: the default domain, else the first assigned one. */
    public Optional<UUID> initialDomainId() {
        if (defaultDomainId != null) return Optional.of(defaultDomainId);
        return domainRoles.stream().findFirst().map(DomainRole::domainId);
    }
}
