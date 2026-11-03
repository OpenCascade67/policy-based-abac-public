package com.pap.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/** A persisted refresh token; rotated on every refresh and revoked on logout or domain switch. */
public class RefreshToken {

    private UUID id = UUID.randomUUID();
    private String token = "";
    private UUID userId;
    private UUID currentDomainId;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt = Instant.now();

    public RefreshToken() {
    }

    public RefreshToken(String token, UUID userId, UUID currentDomainId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.currentDomainId = currentDomainId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getCurrentDomainId() { return currentDomainId; }
    public void setCurrentDomainId(UUID currentDomainId) { this.currentDomainId = currentDomainId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    @JsonProperty("isRevoked")
    public boolean isRevoked() { return revoked; }

    @JsonProperty("isRevoked")
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public boolean isUsableAt(Instant now) {
        return !revoked && expiresAt != null && !expiresAt.isBefore(now);
    }
}
