package com.pap.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Responds 401 with an empty body and a Bearer challenge, which the UI uses to trigger a token refresh. */
@Component
public class BearerAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        Object error = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE);
        String challenge = error instanceof TokenError tokenError ? tokenError.challenge() : "Bearer";
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, challenge);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
