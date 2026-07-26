package com.trendly.backend.message;

import com.trendly.backend.user.User;
import com.trendly.backend.user.UserSummaryDto;

import java.time.Instant;

public record ConversationSummary(
        UserSummaryDto otherUser,
        String lastMessageContent,
        Instant lastMessageAt,
        boolean lastMessageFromMe
) {
    public static ConversationSummary from(Message message, String me) {
        boolean fromMe = message.getSender().getUsername().equals(me);
        User other = fromMe ? message.getRecipient() : message.getSender();

        return new ConversationSummary(
                UserSummaryDto.from(other),
                message.getContent(),
                message.getCreatedAt(),
                fromMe
        );
    }
}
