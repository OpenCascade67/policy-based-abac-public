package com.pap.service;

import com.pap.common.exception.ForbiddenException;
import com.pap.common.exception.NotFoundException;
import com.pap.common.validation.Validator;
import com.pap.config.PapProperties;
import com.pap.domain.model.RefreshToken;
import com.pap.domain.model.User;
import com.pap.domain.model.UserStatus;
import com.pap.domain.model.UserType;
import com.pap.repository.RefreshTokenRepository;
import com.pap.repository.UserRepository;
import com.pap.security.CurrentUser;
import com.pap.security.JwtService;
import com.pap.web.dto.Ids;
import com.pap.web.dto.auth.AuthResponse;
import com.pap.web.dto.auth.UserSummaryDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Sign-in (password or email OTP), token refresh with rotation, domain switching and sign-out. */
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid username or password.";

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final Clock clock;
    private final Duration refreshTokenLifetime;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, JwtService jwtService,
                       OtpService otpService, PasswordEncoder passwordEncoder, CurrentUser currentUser,
                       Clock clock, PapProperties properties) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.clock = clock;
        this.refreshTokenLifetime = Duration.ofDays(properties.jwt().refreshTokenExpiryDays());
    }

    public AuthResponse login(String username, String password) {
        Validator.create()
                .notEmpty("Username", username)
                .notEmpty("Password", password)
                .validate();

        User user = users.findByUsername(username).orElseThrow(() -> new ForbiddenException(INVALID_CREDENTIALS));
        if (user.getUserType() != UserType.LOCAL || !user.isActive()) {
            throw new ForbiddenException("Invalid username or password or account is inactive.");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ForbiddenException(INVALID_CREDENTIALS);
        }
        return startSession(user, user.initialDomainId().orElse(null));
    }

    /** @return the generated code (returned to the caller for demo purposes; production would email it) */
    public String sendOtp(String email) {
        Validator.create()
                .notEmpty("Email", email)
                .email("Email", email)
                .validate();
        return otpService.generate(email);
    }

    /** Verifies an OTP and signs the user in, creating an email user on first sign-in. */
    public AuthResponse verifyOtp(String email, String code) {
        Validator.create()
                .notEmpty("Email", email)
                .email("Email", email)
                .notEmpty("Code", code)
                .exactLength("Code", code, 6)
                .validate();

        if (!otpService.verify(email, code)) throw new ForbiddenException("Invalid OTP code.");
        otpService.clear(email);

        User user = users.findByEmail(email).orElseGet(() -> users.add(newEmailUser(email)));
        if (!user.isActive()) throw new ForbiddenException("Account is inactive.");
        return startSession(user, user.initialDomainId().orElse(null));
    }

    /** Exchanges a refresh token for a new token pair; the presented token is revoked (rotation). */
    public AuthResponse refresh(String token) {
        RefreshToken stored = refreshTokens.findByToken(token)
                .orElseThrow(() -> new ForbiddenException("Invalid refresh token."));
        if (!stored.isUsableAt(clock.instant())) {
            throw new ForbiddenException("Refresh token has expired or been revoked.");
        }
        User user = users.findById(stored.getUserId()).orElseThrow(() -> new ForbiddenException("User not found."));

        refreshTokens.revoke(token);
        return startSession(user, stored.getCurrentDomainId());
    }

    /** Re-issues tokens scoped to another domain the caller is assigned to; other sessions are revoked. */
    public AuthResponse switchDomain(String domainId) {
        UUID userId = currentUser.userIdOrNull();
        User user = Optional.ofNullable(userId).flatMap(users::findById)
                .orElseThrow(() -> new NotFoundException("User", userId));

        UUID targetDomain = Ids.parse(domainId);
        if (!user.isAssignedTo(targetDomain)) throw new ForbiddenException("User is not assigned to this domain.");

        refreshTokens.revokeAllForUser(user.getId());
        return startSession(user, targetDomain);
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    private AuthResponse startSession(User user, UUID domainId) {
        String accessToken = jwtService.generateAccessToken(user, domainId);
        String refreshToken = jwtService.generateRefreshToken();
        refreshTokens.add(new RefreshToken(refreshToken, user.getId(), domainId, clock.instant().plus(refreshTokenLifetime)));
        return new AuthResponse(accessToken, refreshToken, jwtService.accessTokenExpiry(),
                UserSummaryDto.from(user), Ids.format(domainId));
    }

    private User newEmailUser(String email) {
        Instant now = clock.instant();
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(email.split("@", -1)[0]);
        user.setEmail(email);
        user.setUserType(UserType.EMAIL);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setLastModifiedAt(now);
        return user;
    }
}
