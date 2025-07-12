package io.comeandcommue.chat.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.domain.chat.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GlobalChatUseCase {
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_KEY = "chat:messages";
    private static final int MAX_MESSAGES = 1000; // 최대 메시지 수
    private static final int PAGE_SIZE = 100; // 페이지당 메시지 수

    public Mono<Void> save(Message message) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(message))
                .flatMap(json -> redisTemplate.opsForZSet()
                        .add(CHAT_KEY, json, message.getTimestamp()) // score = timestamp
                )
                .then(
                        redisTemplate.opsForZSet().size(CHAT_KEY)
                                .flatMap(size -> {
                                    if (size <= MAX_MESSAGES) return Mono.empty();
                                    long overCount = size - MAX_MESSAGES;
                                    Range<Long> range = Range.closed(0L, overCount - 1); // 오래된 것부터 삭제
                                    return redisTemplate.opsForZSet()
                                            .range(CHAT_KEY, range)
                                            .collectList()
                                            .flatMapMany(Flux::fromIterable)
                                            .flatMap(value -> redisTemplate.opsForZSet().remove(CHAT_KEY, value))
                                            .then();
                                })
                )
                .then();
    }

    public Mono<List<Message>> getMessages(Long beforeTimestamp) {
        double maxScore = beforeTimestamp != null
                ? beforeTimestamp.doubleValue() - 1
                : Double.POSITIVE_INFINITY;

        return redisTemplate.opsForZSet()
                .reverseRangeByScore(CHAT_KEY, Range.closed(0d, maxScore), Limit.limit().count(PAGE_SIZE))
                .flatMap(json -> Mono.fromCallable(() -> objectMapper.readValue(json, Message.class)))
                .collectList();
    }
}
