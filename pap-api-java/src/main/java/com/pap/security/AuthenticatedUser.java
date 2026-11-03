package com.pap.security;

import java.util.UUID;

/**
 * The caller identified by a valid access token.
 *
 * @param domainId the administration domain selected for this session, or {@code null} if none
 */
public record AuthenticatedUser(UUID userId, String username, String userType, UUID domainId) {
}
