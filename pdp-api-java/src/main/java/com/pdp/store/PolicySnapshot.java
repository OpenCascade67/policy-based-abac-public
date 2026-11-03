package com.pdp.store;

import com.pdp.domain.model.Domain;
import com.pdp.domain.model.Policy;
import com.pdp.domain.model.SharedRule;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** An immutable, consistent view of the PAP's policy data, indexed by domain. */
public final class PolicySnapshot {

    private final Map<UUID, Domain> domains;
    private final Map<UUID, List<Policy>> policiesByDomain;
    private final Map<UUID, List<SharedRule>> sharedRulesByDomain;
    private final int policyCount;
    private final int sharedRuleCount;
    private final Instant loadedAt;

    public PolicySnapshot(List<Domain> domains, List<Policy> policies, List<SharedRule> sharedRules, Instant loadedAt) {
        this.domains = domains.stream()
                .filter(d -> d.id() != null)
                .collect(Collectors.toUnmodifiableMap(Domain::id, Function.identity(), (a, b) -> a));
        this.policiesByDomain = policies.stream()
                .collect(Collectors.groupingBy(Policy::effectiveDomainId, Collectors.toUnmodifiableList()));
        this.sharedRulesByDomain = sharedRules.stream()
                .collect(Collectors.groupingBy(SharedRule::effectiveDomainId, Collectors.toUnmodifiableList()));
        this.policyCount = policies.size();
        this.sharedRuleCount = sharedRules.size();
        this.loadedAt = loadedAt;
    }

    public static PolicySnapshot empty(Instant at) {
        return new PolicySnapshot(List.of(), List.of(), List.of(), at);
    }

    public Optional<Domain> domain(UUID id) {
        return Optional.ofNullable(domains.get(id));
    }

    /** Policies of the domain, in the order the PAP stores them. */
    public List<Policy> policiesIn(UUID domainId) {
        return policiesByDomain.getOrDefault(domainId, List.of());
    }

    public List<SharedRule> sharedRulesIn(UUID domainId) {
        return sharedRulesByDomain.getOrDefault(domainId, List.of());
    }

    public int domainCount() {
        return domains.size();
    }

    public int policyCount() {
        return policyCount;
    }

    public int sharedRuleCount() {
        return sharedRuleCount;
    }

    public Instant loadedAt() {
        return loadedAt;
    }
}
