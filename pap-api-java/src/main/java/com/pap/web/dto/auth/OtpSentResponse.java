package com.pap.web.dto.auth;

/**
 * Acknowledges an OTP request. {@code code} is returned for demo purposes only; a production deployment
 * would deliver it by email and omit it here.
 */
public record OtpSentResponse(String message, String code) {
}
