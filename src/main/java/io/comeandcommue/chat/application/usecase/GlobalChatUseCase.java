package io.comeandcommue.chat.application.usecase;

import io.comeandcommue.chat.domain.chat.Message;
import io.comeandcommue.chat.infrastructure.redis.store.GlobalChatRedisStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GlobalChatUseCase {
    private final Logger log = LoggerFactory.getLogger(GlobalChatUseCase.class);

    private final GlobalChatRedisStore globalChatStore;

    public Mono<Boolean> save(Message msg) {
        return globalChatStore.save(msg);
    }

    public Flux<Message> page(int page, int pageSize) {
        return  globalChatStore.page(page, pageSize);
    }
}
