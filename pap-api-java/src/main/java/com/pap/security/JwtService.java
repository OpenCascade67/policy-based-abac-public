package com.pap.security;

import com.pap.config.PapProperties;
import com.pap.domain.model.User;
import com.pap.json.EnumNames;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates HS256 access tokens. Claims ({@code sub}, {@code unique_name}, {@code userType},
 * {@code domain}) and signing match the .NET implementation, so tokens are accepted by either back end.
 */
@Service
public class JwtService {

    static final String CLAIM_USERNAME = "unique_name";
    static final String CLAIM_USER_TYPE = "userType";
    static final String CLAIM_DOMAIN = "domain";

    private static final int REFRESH_TOKEN_BYTES = 64;

    private final SecretKey key;
    private final Duration accessTokenLifetime;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public JwtService(PapProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenLifetime = Duration.ofHours(properties.jwt().accessTokenExpiryHours());
        this.clock = clock;
    }

    public String generateAccessToken(User user, UUID currentDomainId) {
        var builder = Jwts.builder()
                .header().type("JWT").and()
                .subject(user.getId().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_USER_TYPE, EnumNames.lower(user.getUserType()))
                .expiration(Date.from(clock.instant().plus(accessTokenLifetime)));
        if (currentDomainId != null) builder.claim(CLAIM_DOMAIN, currentDomainId.toString());
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    /** An opaque, URL-unsafe random token (base64 of 64 random bytes), as issued by the .NET implementation. */
    public String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Validates signature and expiry (no clock skew) and extracts the caller.
     *
     * @throws JwtException if the token is invalid or expired
     */
    public AuthenticatedUser parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        if (claims.getExpiration() == null) throw new JwtException("Token has no expiration.");
        String domain = claims.get(CLAIM_DOMAIN, String.class);
        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get(CLAIM_USERNAME, String.class),
                claims.get(CLAIM_USER_TYPE, String.class),
                domain == null ? null : UUID.fromString(domain));
    }

    public Instant accessTokenExpiry() {
        return clock.instant().plus(accessTokenLifetime);
    }
}
