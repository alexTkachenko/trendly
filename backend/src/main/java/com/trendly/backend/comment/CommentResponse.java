package com.trendly.backend.comment;

import com.trendly.backend.user.UserSummaryDto;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String content,
        UserSummaryDto author,
        Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                UserSummaryDto.from(comment.getAuthor()),
                comment.getCreatedAt()
        );
    }
}
