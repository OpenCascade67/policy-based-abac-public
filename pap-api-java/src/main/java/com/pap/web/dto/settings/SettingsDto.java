package com.pap.web.dto.settings;

import com.pap.domain.model.UserSettings;

public record SettingsDto(String theme, String colorScheme) {

    public static SettingsDto from(UserSettings settings) {
        return new SettingsDto(settings.getTheme(), settings.getColorScheme());
    }
}
