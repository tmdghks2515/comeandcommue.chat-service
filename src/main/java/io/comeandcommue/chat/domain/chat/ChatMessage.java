package io.comeandcommue.chat.domain.chat;

public record ChatMessage(
        String content,
        String senderId,
        String senderNickname,
        long timestamp
) {}
