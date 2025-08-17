package io.comeandcommue.chat.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.domain.chat.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostCommentUseCase {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String POST_COMMENTS_PREFIX = "post:comments:"; // 동적 키 prefix
    private static final Duration TTL = Duration.ofDays(7); // 1주일

    private String keyOf(String postId) {
        return POST_COMMENTS_PREFIX + postId;
    }

    public Mono<Long> save(Message msg) {
        final String key = keyOf(msg.targetId());
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(msg))
                .flatMap(json -> redisTemplate.opsForList().leftPush(key, json))  // LPUSH
                .flatMap(len -> redisTemplate.expire(key, TTL).thenReturn(len)); // TTL 7일
    }

    public Mono<List<Message>> page(String postId, int page, int pageSize) {
        final String key = keyOf(postId);
        long start = (long) page * pageSize;
        long end = start + pageSize - 1;
        return redisTemplate.opsForList().range(key, start, end)
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readValue(json, Message.class)))
                .collectList();
    }
}
