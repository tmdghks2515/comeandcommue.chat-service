package io.comeandcommue.chat.domain.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GlobalChatStore {
    public Mono<Boolean> save(Message msg);
    public Flux<Message> page(int page, int pageSize);
}
