package com.trendly.backend.post;

import com.trendly.backend.user.UserSummaryDto;

import java.time.Instant;

public record PostResponse(
        Long id,
        String textContent,
        String imageUrl,
        UserSummaryDto author,
        Instant createdAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTextContent(),
                post.getImageUrl(),
                UserSummaryDto.from(post.getAuthor()),
                post.getCreatedAt()
        );
    }
}
