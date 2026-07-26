package com.trendly.backend.post;

import com.trendly.backend.user.UserSummaryDto;

import java.time.Instant;

public record PostResponse(
        Long id,
        String textContent,
        String imageUrl,
        UserSummaryDto author,
        Instant createdAt,
        UserSummaryDto repostedBy,
        Instant repostedAt,
        boolean repostedByMe
) {
    public static PostResponse from(Post post, boolean repostedByMe) {
        boolean isRepost = post.getOriginalPost() != null;
        Post content = isRepost ? post.getOriginalPost() : post;

        return new PostResponse(
                content.getId(),
                content.getTextContent(),
                content.getImageUrl(),
                UserSummaryDto.from(content.getAuthor()),
                content.getCreatedAt(),
                isRepost ? UserSummaryDto.from(post.getAuthor()) : null,
                isRepost ? post.getCreatedAt() : null,
                repostedByMe
        );
    }

    public static PostResponse from(Post post) {
        return from(post, false);
    }
}
