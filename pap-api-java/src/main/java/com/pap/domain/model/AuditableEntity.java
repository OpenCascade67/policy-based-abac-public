package com.pap.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Base class for persisted entities that carry an id and audit metadata. */
public abstract class AuditableEntity {

    private UUID id;
    private Instant createdAt;
    private UUID createdById;
    private Instant lastModifiedAt;
    private UUID lastModifiedById;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UUID getCreatedById() { return createdById; }
    public void setCreatedById(UUID createdById) { this.createdById = createdById; }

    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(Instant lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }

    public UUID getLastModifiedById() { return lastModifiedById; }
    public void setLastModifiedById(UUID lastModifiedById) { this.lastModifiedById = lastModifiedById; }

    /** Stamps creation and modification metadata on a newly created entity. */
    public void markCreated(Instant now, UUID userId) {
        createdAt = now;
        createdById = userId;
        markModified(now, userId);
    }

    public void markModified(Instant now, UUID userId) {
        lastModifiedAt = now;
        lastModifiedById = userId;
    }
}
