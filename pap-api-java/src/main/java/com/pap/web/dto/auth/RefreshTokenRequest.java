package com.pap.web.dto.auth;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(@NotNull String refreshToken) {
}
