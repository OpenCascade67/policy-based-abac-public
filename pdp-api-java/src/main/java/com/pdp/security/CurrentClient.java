package com.pdp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Access to the PEP client that sent the current request. */
@Component
public class CurrentClient {

    /** @throws IllegalStateException outside an authenticated request (security config prevents this) */
    public ApiClient get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ApiClient client) return client;
        throw new IllegalStateException("No authenticated API client");
    }
}
