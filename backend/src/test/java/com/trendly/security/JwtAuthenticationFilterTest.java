package com.trendly.security;

import com.trendly.config.SecurityConfig;
import com.trendly.security.support.ProtectedTestController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security-slice test proving the JWT filter chain end-to-end (BE-1.3
 * acceptance criteria) against a trivial protected endpoint
 * ({@link ProtectedTestController}), since no real protected endpoint
 * exists yet (that's E-BE-2+).
 */
@WebMvcTest(controllers = ProtectedTestController.class)
@Import({SecurityConfig.class, JwtService.class})
@TestPropertySource(properties = "app.jwt.secret=" + JwtAuthenticationFilterTest.TEST_SECRET)
class JwtAuthenticationFilterTest {

    static final String TEST_SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes-long";

    private static final SecretKey SIGNING_KEY =
            Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void noToken_isRejected() throws Exception {
        mockMvc.perform(get("/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validToken_isAccepted() throws Exception {
        String token = validToken("ann");

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void wrongSecret_isRejected() throws Exception {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "a-completely-different-secret-key-that-is-long-enough".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("ann")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredToken_isRejected() throws Exception {
        String token = Jwts.builder()
                .subject("ann")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(SIGNING_KEY)
                .compact();

        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedBearerValue_isRejected() throws Exception {
        mockMvc.perform(get("/test/protected").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    private String validToken(String subject) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(SIGNING_KEY)
                .compact();
    }
}
