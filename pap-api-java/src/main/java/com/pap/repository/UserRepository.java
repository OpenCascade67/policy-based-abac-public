package com.pap.repository;

import com.pap.domain.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository extends JsonFileRepository<User> {

    public UserRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "users.json", User.class, domainProvider);
    }

    @Override
    protected UUID idOf(User entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(User entity, UUID id) {
        entity.setId(id);
    }

    public Optional<User> findByUsername(String username) {
        return findAll().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst();
    }

    public Optional<User> findByEmail(String email) {
        return findAll().stream().filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }
}
