package com.pap.web.dto.settings;

import com.pap.domain.model.UserSettings;

public record SettingsRequest(String theme, String colorScheme) {

    public SettingsRequest {
        theme = theme == null ? UserSettings.DEFAULT_THEME : theme;
        colorScheme = colorScheme == null ? UserSettings.DEFAULT_COLOR_SCHEME : colorScheme;
    }
}
