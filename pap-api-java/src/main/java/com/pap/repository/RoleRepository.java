package com.pap.repository;

import com.pap.domain.model.Role;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RoleRepository extends JsonFileRepository<Role> {

    public RoleRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "roles.json", Role.class, domainProvider);
    }

    @Override
    protected UUID idOf(Role entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(Role entity, UUID id) {
        entity.setId(id);
    }

    public boolean existsByName(String name) {
        return findAll().stream().anyMatch(r -> r.getName().equalsIgnoreCase(name));
    }
}
