package com.pap.repository;

import com.pap.domain.model.Domain;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class DomainRepository extends JsonFileRepository<Domain> {

    public DomainRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "domains.json", Domain.class, domainProvider);
    }

    @Override
    protected UUID idOf(Domain entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(Domain entity, UUID id) {
        entity.setId(id);
    }

    public boolean existsByName(String name) {
        return findAll().stream().anyMatch(d -> d.getName().equalsIgnoreCase(name));
    }
}
