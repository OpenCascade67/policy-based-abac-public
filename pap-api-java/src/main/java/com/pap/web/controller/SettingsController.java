package com.pap.web.controller;

import com.pap.service.SettingsService;
import com.pap.web.dto.settings.SettingsDto;
import com.pap.web.dto.settings.SettingsRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The calling user's UI preferences. */
@Tag(name = "Settings")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsDto get() {
        return settingsService.get();
    }

    @PutMapping
    public SettingsDto update(@RequestBody SettingsRequest request) {
        return settingsService.update(request);
    }
}
