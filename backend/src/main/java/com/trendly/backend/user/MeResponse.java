package com.trendly.backend.user;

import java.time.Instant;

public record MeResponse(
        Long id,
        String email,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        Instant createdAt
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }
}
