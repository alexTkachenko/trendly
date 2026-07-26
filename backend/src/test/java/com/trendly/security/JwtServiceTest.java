package com.trendly.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JwtService}: signature + expiry validation only --
 * token issuance is E-BE-2's job, so tokens here are built by hand with the
 * jjwt library directly, not via any production issuance code.
 */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";
    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void validToken_returnsSubject() {
        String token = Jwts.builder()
                .subject("ann")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(SIGNING_KEY)
                .compact();

        assertThat(jwtService.validateAndGetSubject(token)).contains("ann");
    }

    @Test
    void wrongSigningKey_isRejected() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-that-is-long-enough".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("ann")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        assertThat(jwtService.validateAndGetSubject(token)).isEmpty();
    }

    @Test
    void expiredToken_isRejected() {
        String token = Jwts.builder()
                .subject("ann")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(SIGNING_KEY)
                .compact();

        assertThat(jwtService.validateAndGetSubject(token)).isEmpty();
    }

    @Test
    void malformedToken_isRejected() {
        assertThat(jwtService.validateAndGetSubject("not-a-jwt")).isEmpty();
    }

    @Test
    void blankToken_isRejected() {
        assertThat(jwtService.validateAndGetSubject("")).isEmpty();
        assertThat(jwtService.validateAndGetSubject(null)).isEmpty();
    }
}
