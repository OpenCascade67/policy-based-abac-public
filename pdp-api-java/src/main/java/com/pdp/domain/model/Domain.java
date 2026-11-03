package com.pdp.domain.model;

import java.util.UUID;

/** An administration domain as stored by the PAP ({@code domains.json}). */
public record Domain(UUID id, String name, DomainStatus status) {

    public Domain {
        status = status == null ? DomainStatus.ACTIVE : status;
    }

    public boolean isActive() {
        return status == DomainStatus.ACTIVE;
    }
}
