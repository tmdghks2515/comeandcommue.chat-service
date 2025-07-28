package io.comeandcommue.chat.domain.chat;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Builder(builderMethodName = "of")
public class ChatMessage {
    private String content;
    private String sender;
    private String ip;
    private long timestamp;
}
