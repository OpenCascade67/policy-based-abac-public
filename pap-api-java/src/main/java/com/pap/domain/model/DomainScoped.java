package com.pap.domain.model;

import java.util.UUID;

/** Entities that belong to one administration domain and are filtered by the caller's current domain. */
public interface DomainScoped {

    /** Marker for entities not yet assigned to a domain (Guid.Empty in the .NET data files). */
    UUID UNASSIGNED = new UUID(0L, 0L);

    UUID getDomainId();

    void setDomainId(UUID domainId);
}
