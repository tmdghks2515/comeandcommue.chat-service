package io.comeandcommue.chat.application.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder(builderMethodName = "of")
@Getter
public class PostCommentCreatedEvent {
    private String postId;
}
