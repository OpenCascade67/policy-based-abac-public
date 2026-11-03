package com.pap.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/** Typed application settings bound from the {@code pap.*} properties. */
@Validated
@ConfigurationProperties(prefix = "pap")
public record PapProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull DataStore dataStore, @Valid @NotNull Cors cors) {

    /**
     * @param secret                  HMAC-SHA256 signing key; at least 32 bytes
     * @param accessTokenExpiryHours  lifetime of access tokens
     * @param refreshTokenExpiryDays  lifetime of refresh tokens
     */
    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @Min(1) int accessTokenExpiryHours,
            @Min(1) int refreshTokenExpiryDays) {
    }

    /** @param folder directory holding the JSON data files; relative paths resolve against the working directory */
    public record DataStore(@NotBlank String folder) {
    }

    /** @param allowedOriginPatterns origins allowed to call the API, e.g. {@code https://app-pap-ui-*.azurewebsites.net} */
    public record Cors(@NotNull List<String> allowedOriginPatterns) {
    }
}
