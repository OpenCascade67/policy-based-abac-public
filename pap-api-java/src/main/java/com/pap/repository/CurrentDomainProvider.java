package com.pap.repository;

import java.util.Optional;
import java.util.UUID;

/** Supplies the caller's current administration domain, used to scope domain-owned data. */
@FunctionalInterface
public interface CurrentDomainProvider {

    /** @return the current domain, or empty outside an authenticated request (e.g. during seeding) */
    Optional<UUID> currentDomainId();
}
