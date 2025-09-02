package io.comeandcommue.chat.infrastructure.redis.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.application.event.EventPublisher;
import io.comeandcommue.chat.application.event.PostCommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class RedisEventPublisher implements EventPublisher {
    private final Logger log = LoggerFactory.getLogger(RedisEventPublisher.class);
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Long> publishPostCommentCreatedEvent(String postId) {
        return redisTemplate.convertAndSend("post.comment.created", postId);
    }
}
