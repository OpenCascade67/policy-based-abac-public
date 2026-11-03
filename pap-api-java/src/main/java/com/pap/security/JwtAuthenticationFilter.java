package com.pap.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying {@code Authorization: Bearer <token>}. Invalid tokens leave the request
 * anonymous (so public endpoints still work); the failure is recorded for {@link BearerAuthenticationEntryPoint}.
 * Registered only inside the security filter chain, not as a standalone servlet filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String TOKEN_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                AuthenticatedUser user = jwtService.parseAccessToken(token);
                var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                request.setAttribute(TOKEN_ERROR_ATTRIBUTE, TokenError.expired(e.getClaims().getExpiration().toInstant()));
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(TOKEN_ERROR_ATTRIBUTE, TokenError.invalid());
            }
        }
        chain.doFilter(request, response);
    }
}
