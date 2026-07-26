package com.trendly.backend.message;

import com.trendly.backend.user.UserSummaryDto;

import java.time.Instant;

public record MessageResponse(
        Long id,
        UserSummaryDto sender,
        UserSummaryDto recipient,
        String content,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                UserSummaryDto.from(message.getSender()),
                UserSummaryDto.from(message.getRecipient()),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
