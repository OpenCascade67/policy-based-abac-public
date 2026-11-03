package com.pap.repository;

import com.pap.domain.model.Policy;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
public class PolicyRepository extends JsonFileRepository<Policy> {

    public PolicyRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "policies.json", Policy.class, domainProvider);
    }

    @Override
    protected UUID idOf(Policy entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(Policy entity, UUID id) {
        entity.setId(id);
    }

    /** Policies whose name contains {@code search} (case-insensitive), ordered by name. */
    public List<Policy> search(String search) {
        String term = search == null || search.isBlank() ? null : search.toLowerCase(Locale.ROOT);
        return findAll().stream()
                .filter(p -> term == null || p.getName().toLowerCase(Locale.ROOT).contains(term))
                .sorted(Comparator.comparing(Policy::getName, NameOrder.INSTANCE))
                .toList();
    }
}
