package com.pap.security;

import com.pap.repository.CurrentDomainProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Access to the authenticated caller of the current request. */
@Component
public class CurrentUser implements CurrentDomainProvider {

    public Optional<AuthenticatedUser> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /** @return the caller's user id, or {@code null} outside an authenticated request */
    public UUID userIdOrNull() {
        return get().map(AuthenticatedUser::userId).orElse(null);
    }

    @Override
    public Optional<UUID> currentDomainId() {
        return get().map(AuthenticatedUser::domainId);
    }
}
