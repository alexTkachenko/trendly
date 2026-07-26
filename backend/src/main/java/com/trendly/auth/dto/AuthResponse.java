package com.trendly.auth.dto;

/**
 * Response body shared by both {@code POST /auth/register} (201) and
 * {@code POST /auth/login} (200) per {@code docs/api/auth.md} -- same
 * shape either way.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        UserSummary user
) {
}
