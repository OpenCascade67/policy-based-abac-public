package com.pap.web.dto.auth;

import jakarta.validation.constraints.NotNull;

public record SwitchDomainRequest(@NotNull String domainId) {
}
