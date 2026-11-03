package com.pap.repository;

import com.pap.domain.model.Scenario;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ScenarioRepository extends JsonFileRepository<Scenario> {

    public ScenarioRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "scenarios.json", Scenario.class, domainProvider);
    }

    @Override
    protected UUID idOf(Scenario entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(Scenario entity, UUID id) {
        entity.setId(id);
    }
}
