package com.pap.repository;

import com.pap.domain.model.SharedRule;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class SharedRuleRepository extends JsonFileRepository<SharedRule> {

    public SharedRuleRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "shared-rules.json", SharedRule.class, domainProvider);
    }

    @Override
    protected UUID idOf(SharedRule entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(SharedRule entity, UUID id) {
        entity.setId(id);
    }
}
