package com.trendly.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates JWT access tokens signed with {@code app.jwt.secret}
 * (BE-1.3 added validation; BE-2.2 added issuance -- kept on this one class
 * per BE-2.2's task notes, rather than duplicating signing logic
 * elsewhere).
 */
@Component
public class JwtService {

    /**
     * Access token lifetime. Implementation default (no ADR) picked by
     * BE-2.2: 24 hours -- long enough that MVP users aren't forced to
     * re-login mid-session, short enough to bound a leaked token's blast
     * radius given there's no refresh-token/revocation mechanism yet (see
     * the "Open decision" note in {@code docs/api/auth.md}).
     */
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(24);

    private final SecretKey signingKey;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Issues a signed access token for the given user id (BE-2.2). The
     * subject claim ({@code sub}) is the user's id, as a string -- matches
     * {@link #validateAndGetSubject(String)}'s contract of returning the
     * subject claim verbatim.
     */
    public String issueToken(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ACCESS_TOKEN_TTL)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the token's signature and expiry and returns the subject
     * (username) claim if valid. Returns empty -- never throws -- for any
     * kind of invalid token (bad signature, expired, malformed, blank).
     * Callers use this to decide whether to populate the security context;
     * they must not reject the request themselves, that's Spring
     * Security's job based on the resulting (un)authenticated state.
     */
    public Optional<String> validateAndGetSubject(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
