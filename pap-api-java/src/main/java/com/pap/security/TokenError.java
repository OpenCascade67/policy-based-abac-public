package com.pap.security;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Why a presented bearer token was rejected, rendered as an RFC 6750 {@code WWW-Authenticate} challenge. */
record TokenError(Instant expiredAt) {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").withZone(ZoneOffset.UTC);

    static TokenError invalid() {
        return new TokenError(null);
    }

    static TokenError expired(Instant expiredAt) {
        return new TokenError(expiredAt);
    }

    String challenge() {
        if (expiredAt == null) return "Bearer error=\"invalid_token\"";
        return "Bearer error=\"invalid_token\", error_description=\"The token expired at '%s'\""
                .formatted(EXPIRY_FORMAT.format(expiredAt));
    }
}
