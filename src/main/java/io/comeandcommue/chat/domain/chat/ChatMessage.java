package io.comeandcommue.chat.domain.chat;

import java.time.Instant;

public record ChatMessage(
        String content,
        String senderId,
        String senderNickname,
        long timestamp
) {
    public ChatMessage {
        if (timestamp == 0) {
            timestamp = Instant.now().toEpochMilli();
        }
    }
}
