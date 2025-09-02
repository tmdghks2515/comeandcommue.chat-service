package io.comeandcommue.chat.domain.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PostCommentStore {
    public Mono<Boolean> save(Message msg);
    public Flux<Message> page(String postId, int page, int pageSize);
}
