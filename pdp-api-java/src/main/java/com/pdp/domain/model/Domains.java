package com.pdp.domain.model;

import java.util.UUID;

/** Domain conventions shared with the PAP data store. */
public final class Domains {

    /** Well-known id of the PAP's default domain. */
    public static final UUID DEFAULT_DOMAIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /** Marker the PAP uses for data not yet assigned to a domain (Guid.Empty). */
    public static final UUID UNASSIGNED = new UUID(0L, 0L);

    private Domains() {
    }

    /** Data created before domains existed belongs to the default domain, as in the PAP. */
    public static UUID effective(UUID domainId) {
        return domainId == null || UNASSIGNED.equals(domainId) ? DEFAULT_DOMAIN_ID : domainId;
    }
}
