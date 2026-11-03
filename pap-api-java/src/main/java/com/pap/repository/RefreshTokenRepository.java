package com.pap.repository;

import com.pap.domain.model.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepository extends JsonFileRepository<RefreshToken> {

    public RefreshTokenRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "refresh-tokens.json", RefreshToken.class, domainProvider);
    }

    @Override
    protected UUID idOf(RefreshToken entity) {
        return entity.getId();
    }

    @Override
    protected void assignId(RefreshToken entity, UUID id) {
        entity.setId(id);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return findAllUnscoped().stream().filter(t -> t.getToken().equals(token)).findFirst();
    }

    public void revoke(String token) {
        modifyAll(all -> {
            all.stream().filter(t -> t.getToken().equals(token)).forEach(t -> t.setRevoked(true));
            return null;
        });
    }

    public void revokeAllForUser(UUID userId) {
        modifyAll(all -> {
            all.stream().filter(t -> userId.equals(t.getUserId())).forEach(t -> t.setRevoked(true));
            return null;
        });
    }
}
