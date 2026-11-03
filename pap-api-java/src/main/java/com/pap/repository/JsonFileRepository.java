package com.pap.repository;

import com.fasterxml.jackson.databind.JavaType;
import com.pap.domain.model.Domain;
import com.pap.domain.model.DomainScoped;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Generic repository over one JSON file. Entities implementing {@link DomainScoped} are filtered to the
 * caller's current domain on read and stamped with it on create; writes always operate on the full collection.
 */
public abstract class JsonFileRepository<T> {

    private final JsonFileStore store;
    private final String fileName;
    private final Class<T> type;
    private final JavaType listType;
    private final CurrentDomainProvider domainProvider;

    protected JsonFileRepository(JsonFileStore store, String fileName, Class<T> type, CurrentDomainProvider domainProvider) {
        this.store = store;
        this.fileName = fileName;
        this.type = type;
        this.listType = store.mapper().getTypeFactory().constructCollectionType(List.class, type);
        this.domainProvider = domainProvider;
    }

    protected abstract UUID idOf(T entity);

    protected abstract void assignId(T entity, UUID id);

    /** All entities visible to the caller (scoped to the current domain for domain-owned types). */
    public List<T> findAll() {
        List<T> all = findAllUnscoped();
        Optional<UUID> currentDomain = domainProvider.currentDomainId();
        if (currentDomain.isEmpty() || !isDomainScoped()) return all;

        UUID domainId = currentDomain.get();
        return all.stream()
                .map(this::withLegacyDomainAssigned)
                .filter(e -> domainId.equals(((DomainScoped) e).getDomainId()))
                .toList();
    }

    public Optional<T> findById(UUID id) {
        return findAll().stream().filter(e -> id.equals(idOf(e))).findFirst();
    }

    /** Existence check across all domains. */
    public boolean existsById(UUID id) {
        return findAllUnscoped().stream().anyMatch(e -> id.equals(idOf(e)));
    }

    public T add(T entity) {
        return store.<T, T>modify(fileName, listType, all -> {
            if (idOf(entity) == null) assignId(entity, UUID.randomUUID());
            Optional<UUID> currentDomain = domainProvider.currentDomainId();
            if (entity instanceof DomainScoped scoped && currentDomain.isPresent()) {
                scoped.setDomainId(currentDomain.get());
            }
            all.add(entity);
            return entity;
        });
    }

    public T update(T entity) {
        return store.<T, T>modify(fileName, listType, all -> {
            UUID id = idOf(entity);
            int index = indexOf(all, id);
            if (index < 0) throw new NoSuchElementException("Entity " + id + " not found.");
            Optional<UUID> currentDomain = domainProvider.currentDomainId();
            if (entity instanceof DomainScoped scoped && DomainScoped.UNASSIGNED.equals(scoped.getDomainId())
                    && currentDomain.isPresent()) {
                scoped.setDomainId(currentDomain.get());
            }
            all.set(index, entity);
            return entity;
        });
    }

    public void deleteById(UUID id) {
        store.<T, Void>modify(fileName, listType, all -> {
            if (!all.removeIf(e -> id.equals(idOf(e)))) throw new NoSuchElementException("Entity " + id + " not found.");
            return null;
        });
    }

    /** All entities regardless of domain. */
    protected List<T> findAllUnscoped() {
        return store.read(fileName, listType);
    }

    /** Applies an in-place change to the full collection under the file's write lock. */
    protected <R> R modifyAll(Function<List<T>, R> change) {
        return store.modify(fileName, listType, change);
    }

    private boolean isDomainScoped() {
        return DomainScoped.class.isAssignableFrom(type);
    }

    /** Data created before domains existed belongs to the default domain rather than to whoever reads it first. */
    private T withLegacyDomainAssigned(T entity) {
        DomainScoped scoped = (DomainScoped) entity;
        if (DomainScoped.UNASSIGNED.equals(scoped.getDomainId())) scoped.setDomainId(Domain.DEFAULT_DOMAIN_ID);
        return entity;
    }

    private int indexOf(List<T> items, UUID id) {
        for (int i = 0; i < items.size(); i++) {
            if (id.equals(idOf(items.get(i)))) return i;
        }
        return -1;
    }
}
