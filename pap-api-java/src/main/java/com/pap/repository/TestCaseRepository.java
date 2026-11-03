package com.pap.repository;

import com.pap.domain.model.TestCase;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class TestCaseRepository extends JsonFileRepository<TestCase> {

    public TestCaseRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "test-cases.json", TestCase.class, domainProvider);
    }

    @Override
    protected UUID idOf(TestCase entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(TestCase entity, UUID id) {
        entity.setId(id);
    }
}
