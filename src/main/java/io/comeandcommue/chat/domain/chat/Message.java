package io.comeandcommue.chat.domain.chat;

import java.time.Instant;

public record Message(
        String content,
        String senderId,
        String senderNickname,
        long timestamp,
        MessageType messageType,
        String targetId
) {
    public Message {
        if (timestamp == 0) {
            timestamp = Instant.now().toEpochMilli();
        }
        if (messageType == null) {
            messageType = MessageType.GLOBAL_CHAT;
        }
    }
}
