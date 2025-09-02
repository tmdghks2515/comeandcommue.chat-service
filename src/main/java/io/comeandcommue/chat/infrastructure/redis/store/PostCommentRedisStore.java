package io.comeandcommue.chat.infrastructure.redis.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.domain.chat.Message;
import io.comeandcommue.chat.domain.chat.PostCommentStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class PostCommentRedisStore implements PostCommentStore {
    private final Logger log = LoggerFactory.getLogger(PostCommentRedisStore.class);
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String POST_COMMENTS_PREFIX = "post:comments:"; // 동적 키 prefix
    private static final Duration TTL = Duration.ofDays(7); // 1주일

    private String keyOf(String postId) {
        return POST_COMMENTS_PREFIX + postId;
    }

    @Override
    public Mono<Boolean> save(Message msg) {
        final String key = keyOf((String) msg.target().getOrDefault("id", "unkown"));
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(msg))
                .flatMap(json -> redisTemplate.opsForList().leftPush(key, json))  // LPUSH
                .flatMap(len -> redisTemplate.expire(key, TTL)); // TTL 7일
    }

    @Override
    public Flux<Message> page(String postId, int page, int pageSize) {
        final String key = keyOf(postId);
        long start = (long) page * pageSize;
        long end   = start + pageSize - 1;

        return redisTemplate.opsForList()
                .range(key, start, end) // Flux<String> (JSON)
                .flatMap(json ->
                        Mono.fromCallable(() -> objectMapper.readValue(json, Message.class))
                                .onErrorResume(e -> {
                                    log.warn("Bad message json: {}", json, e);
                                    return Mono.empty(); // 해당 아이템만 스킵
                                })
                );
    }
}
