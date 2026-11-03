package com.pdp.security;

import java.util.Set;
import java.util.UUID;

/**
 * An authenticated PEP application.
 *
 * @param allowedDomains domains the client may query; empty means all domains
 */
public record ApiClient(String name, Set<UUID> allowedDomains) {

    public ApiClient {
        allowedDomains = Set.copyOf(allowedDomains);
    }

    public boolean mayAccess(UUID domainId) {
        return allowedDomains.isEmpty() || allowedDomains.contains(domainId);
    }
}
