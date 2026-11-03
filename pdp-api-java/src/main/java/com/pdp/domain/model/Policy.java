package com.pdp.domain.model;

import java.util.UUID;

/** A policy as stored by the PAP ({@code policies.json}); audit fields are not needed for decisions. */
public record Policy(UUID id, UUID domainId, String name, String description, PolicyDefinition policyDefinition) {

    public Policy {
        name = name == null ? "" : name;
        policyDefinition = policyDefinition == null
                ? new PolicyDefinition(null, null, null, null, null, null)
                : policyDefinition;
    }

    public UUID effectiveDomainId() {
        return Domains.effective(domainId);
    }
}
