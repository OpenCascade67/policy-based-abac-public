package com.pap.web.controller;

import com.pap.service.AuthService;
import com.pap.web.dto.auth.AuthResponse;
import com.pap.web.dto.auth.LoginRequest;
import com.pap.web.dto.auth.OtpSentResponse;
import com.pap.web.dto.auth.RefreshTokenRequest;
import com.pap.web.dto.auth.SendOtpRequest;
import com.pap.web.dto.auth.SwitchDomainRequest;
import com.pap.web.dto.auth.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Sign in with username and password")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @Operation(summary = "Send a one-time sign-in code to an email address")
    @PostMapping("/otp/send")
    public OtpSentResponse sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return new OtpSentResponse("OTP sent", authService.sendOtp(request.email()));
    }

    @Operation(summary = "Sign in with an emailed one-time code")
    @PostMapping("/otp/verify")
    public AuthResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return authService.verifyOtp(request.email(), request.code());
    }

    @Operation(summary = "Exchange a refresh token for new tokens")
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Re-issue tokens for another assigned domain")
    @PostMapping("/switch-domain")
    public AuthResponse switchDomain(@Valid @RequestBody SwitchDomainRequest request) {
        return authService.switchDomain(request.domainId());
    }

    @Operation(summary = "Revoke a refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
