package com.trendly.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Populates the security context from a bearer JWT, if one is present and
 * valid (BE-1.3).
 *
 * Deliberately never rejects the request itself: a missing, malformed,
 * expired, or wrong-signature token just leaves the request unauthenticated
 * and lets the chain continue. {@link com.trendly.config.SecurityConfig}'s
 * authorization rules then turn that into a 401 for protected routes,
 * while permitAll routes (e.g. /actuator/health) are unaffected either way.
 *
 * Not a {@code @Component}: it's registered as a {@code @Bean} directly in
 * {@code SecurityConfig} instead, so it (and its {@link JwtService}
 * dependency) don't leak into unrelated {@code @WebMvcTest} slices via
 * classpath scanning -- those slices already exclude plain
 * {@code @Configuration} classes like {@code SecurityConfig} by default.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        extractToken(request)
                .flatMap(jwtService::validateAndGetSubject)
                .ifPresent(username -> {
                    var authentication =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }
}
