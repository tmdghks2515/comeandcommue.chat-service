package io.comeandcommue.chat.application.event;

import reactor.core.publisher.Mono;

public interface EventPublisher {
    public Mono<Long> publishPostCommentCreatedEvent(String postId);
}
