package com.pap.web.dto.auth;

import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(@NotNull String email) {
}
