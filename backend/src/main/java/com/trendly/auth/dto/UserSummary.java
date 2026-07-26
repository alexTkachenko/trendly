package com.trendly.auth.dto;

/**
 * Public-facing view of a {@link com.trendly.user.User}, embedded in
 * {@link AuthResponse}. Deliberately excludes {@code password_hash} (and
 * every other internal-only column) -- entities are never returned
 * directly over the API.
 */
public record UserSummary(
        Long id,
        String username,
        String email,
        String displayName,
        String avatarUrl
) {
}
