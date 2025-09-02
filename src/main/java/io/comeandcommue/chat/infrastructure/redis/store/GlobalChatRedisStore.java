package io.comeandcommue.chat.infrastructure.redis.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.application.usecase.GlobalChatUseCase;
import io.comeandcommue.chat.domain.chat.Message;
import io.comeandcommue.chat.domain.chat.GlobalChatStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class GlobalChatRedisStore implements GlobalChatStore {
    private final Logger log = LoggerFactory.getLogger(GlobalChatUseCase.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String GLOBAL_CHAT_MESSAGES = "chat:messages";
    private static final int MAX_GLOBAL_MESSAGES = 1000; // 최대 메시지 수
    private static final int PAGE_SIZE = 30; // 페이지당 메시지 수

    @Override
    public Mono<Boolean> save(Message msg) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(msg))
                .flatMap(json -> redisTemplate.opsForList().leftPush(GLOBAL_CHAT_MESSAGES, json))
                .flatMap(len -> redisTemplate.opsForList()
                        .trim(GLOBAL_CHAT_MESSAGES, 0, MAX_GLOBAL_MESSAGES - 1)
                );
    }

    @Override
    public Flux<Message> page(int page, int pageSize) {
        long start = (long) page * pageSize;
        long end = start + pageSize - 1;

        return redisTemplate.opsForList()
                .range(GLOBAL_CHAT_MESSAGES, start, end)
                .flatMap(json ->
                        Mono.fromCallable(() -> objectMapper.readValue(json, Message.class))
                                .onErrorResume(e -> {
                                    log.warn("Bad message json: {}", json, e);
                                    return Mono.empty(); // 그 아이템만 스킵
                                })
                );
    }
}
