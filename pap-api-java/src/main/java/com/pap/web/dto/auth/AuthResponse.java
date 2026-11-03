package com.pap.web.dto.auth;

import java.time.Instant;

/** Tokens for a new session plus the signed-in user. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        UserSummaryDto user,
        String currentDomainId) {
}
