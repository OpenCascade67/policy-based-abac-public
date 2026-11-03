package com.pap.domain.model;

import java.util.UUID;

/** Per-user UI preferences, keyed by user id. */
public class UserSettings {

    public static final String DEFAULT_THEME = "light";
    public static final String DEFAULT_COLOR_SCHEME = "blue";

    private UUID userId;
    private String theme = DEFAULT_THEME;
    private String colorScheme = DEFAULT_COLOR_SCHEME;

    public UserSettings() {
    }

    public UserSettings(UUID userId, String theme, String colorScheme) {
        this.userId = userId;
        this.theme = theme;
        this.colorScheme = colorScheme;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getColorScheme() { return colorScheme; }
    public void setColorScheme(String colorScheme) { this.colorScheme = colorScheme; }
}
