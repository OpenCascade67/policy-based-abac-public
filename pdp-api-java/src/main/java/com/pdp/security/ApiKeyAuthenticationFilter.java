package com.pdp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests carrying a valid {@code X-API-Key} header. Requests without a valid key stay anonymous
 * and are rejected by {@link ApiKeyAuthenticationEntryPoint} where authentication is required.
 * Registered only inside the security filter chain, not as a standalone servlet filter.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyRegistry registry;

    public ApiKeyAuthenticationFilter(ApiKeyRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        registry.authenticate(request.getHeader(API_KEY_HEADER)).ifPresent(client ->
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(client, null, List.of())));
        chain.doFilter(request, response);
    }
}
