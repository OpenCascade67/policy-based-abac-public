package com.pap.web.dto.auth;

import jakarta.validation.constraints.NotNull;

public record VerifyOtpRequest(@NotNull String email, @NotNull String code) {
}
