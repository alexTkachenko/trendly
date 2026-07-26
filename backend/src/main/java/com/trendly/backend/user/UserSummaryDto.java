package com.trendly.backend.user;

public record UserSummaryDto(
        Long id,
        String username,
        String displayName,
        String avatarUrl
) {
    public static UserSummaryDto from(User user) {
        return new UserSummaryDto(user.getId(), user.getUsername(), user.getDisplayName(), user.getAvatarUrl());
    }
}
