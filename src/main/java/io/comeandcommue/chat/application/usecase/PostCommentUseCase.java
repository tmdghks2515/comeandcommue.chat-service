package io.comeandcommue.chat.application.usecase;

import io.comeandcommue.chat.application.event.EventPublisher;
import io.comeandcommue.chat.application.event.PostCommentCreatedEvent;
import io.comeandcommue.chat.domain.chat.Message;
import io.comeandcommue.chat.infrastructure.redis.store.PostCommentRedisStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostCommentUseCase {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final PostCommentRedisStore postCommentStore;
    private final EventPublisher eventPublisher;

    public Mono<Void> save(Message msg) {
        return Mono.defer(() -> {
                    // 1) 안전한 postId 추출/검증
                    String postId = Optional.ofNullable(msg.target())
                            .map(t -> (String) t.get("id"))
                            .filter(id -> !id.isBlank())
                            .orElseThrow(() -> new IllegalArgumentException("Missing target.id"));

                    // 2) 저장 → 3) 저장 성공 시 이벤트 발행 → 4) 종료
                    return postCommentStore.save(msg) // Mono<PostComment>
                            .doOnNext(saved -> log.info("[SAVE] stored: {}", saved))
                            .flatMap(saved -> eventPublisher.publishPostCommentCreatedEvent(postId)) // Mono<Long>
                            .doOnNext(n -> log.info("[SAVE] pub subscribers={}", n))
                            .then(); // Mono<Void>
                })
                // 5) 공통 에러 로깅/전파
                .doOnError(e -> log.error("Failed to save comment or publish event, reason={}",
                        e.toString(), e));
    }

    public Flux<Message> page(String postId, int page, int pageSize) {
        return  postCommentStore.page(postId, page, pageSize);
    }
}
