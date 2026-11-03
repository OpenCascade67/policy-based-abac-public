package com.pap.repository;

import com.pap.domain.model.UserSettings;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Settings are keyed by user id. */
@Repository
public class UserSettingsRepository extends JsonFileRepository<UserSettings> {

    public UserSettingsRepository(JsonFileStore store, CurrentDomainProvider domainProvider) {
        super(store, "settings.json", UserSettings.class, domainProvider);
    }

    @Override
    protected UUID idOf(UserSettings entity) {
        return entity.getUserId();
    }

    @Override
    protected void assignId(UserSettings entity, UUID id) {
        entity.setUserId(id);
    }

    public Optional<UserSettings> findByUserId(UUID userId) {
        return findById(userId);
    }

    /** Creates or replaces the settings of {@code settings.getUserId()}. */
    public UserSettings upsert(UserSettings settings) {
        return modifyAll(all -> {
            all.removeIf(s -> settings.getUserId().equals(s.getUserId()));
            all.add(settings);
            return settings;
        });
    }
}
