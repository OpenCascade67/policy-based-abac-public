package com.pdp.store;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Reports whether the policy data folder is readable, and what is currently loaded ({@code /actuator/health}). */
@Component("policyStore")
public class PolicyStoreHealthIndicator implements HealthIndicator {

    private final PolicyStore store;

    public PolicyStoreHealthIndicator(PolicyStore store) {
        this.store = store;
    }

    @Override
    public Health health() {
        PolicyStore.StoreStatus status = store.status();
        PolicySnapshot snapshot = status.snapshot();
        Health.Builder health = status.available() ? Health.up() : Health.down();
        health.withDetail("domains", snapshot.domainCount())
                .withDetail("policies", snapshot.policyCount())
                .withDetail("sharedRules", snapshot.sharedRuleCount())
                .withDetail("loadedAt", snapshot.loadedAt().toString());
        if (status.lastError() != null) health.withDetail("lastError", status.lastError());
        return health.build();
    }
}
