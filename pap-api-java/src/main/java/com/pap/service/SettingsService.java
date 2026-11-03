package com.pap.service;

import com.pap.domain.model.UserSettings;
import com.pap.repository.UserSettingsRepository;
import com.pap.security.CurrentUser;
import com.pap.web.dto.settings.SettingsDto;
import com.pap.web.dto.settings.SettingsRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** The calling user's UI preferences; defaults apply until the user saves their own. */
@Service
public class SettingsService {

    private final UserSettingsRepository settings;
    private final CurrentUser currentUser;

    public SettingsService(UserSettingsRepository settings, CurrentUser currentUser) {
        this.settings = settings;
        this.currentUser = currentUser;
    }

    public SettingsDto get() {
        UUID userId = currentUser.userIdOrNull();
        return settings.findByUserId(userId)
                .map(SettingsDto::from)
                .orElseGet(() -> SettingsDto.from(new UserSettings()));
    }

    public SettingsDto update(SettingsRequest request) {
        UserSettings saved = settings.upsert(new UserSettings(currentUser.userIdOrNull(), request.theme(), request.colorScheme()));
        return SettingsDto.from(saved);
    }
}
