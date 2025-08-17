package io.comeandcommue.chat.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.application.GlobalChatUseCase;
import io.comeandcommue.chat.application.PostCommentUseCase;
import io.comeandcommue.chat.domain.chat.Message;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalChatWebSocketHandler implements WebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalChatWebSocketHandler.class);
    private final GlobalChatUseCase globalChatUseCase;
    private final PostCommentUseCase postCommentUseCase;

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    @Override
    public @NonNull Mono<Void> handle(@NonNull WebSocketSession session) {
        sessions.add(session);
        log.debug("Client connected: {}", session.getId());

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> Mono.fromCallable(() -> parseAndValidate(payload))) // 파싱 + 검증
                .flatMap(this::broadcast)
                .onErrorResume(ex -> handleError(session, ex))
                .then()
                .doFinally(signal -> {
                    sessions.remove(session);
                    log.debug("Client disconnected: {}", session.getId());
                });
    }

    private Message parseAndValidate(String payload) throws JsonProcessingException {
        Message message = objectMapper.readValue(payload, Message.class);

        if (message.content() == null || message.content().isBlank()) {
            throw new IllegalArgumentException("content required");
        }
        if (message.senderId() == null || message.senderId().isBlank()) {
            throw new IllegalArgumentException("senderId required");
        }
        if (message.senderNickname() == null || message.senderNickname().isBlank()) {
            throw new IllegalArgumentException("senderNickname required");
        }

        return message;
    }

    private Mono<Void> broadcast(Message msg) {
        logger.debug("[GlobalChatWebSocketHandler] broadcast >> {}", msg);

        // 1) JSON 직렬화는 블로킹 가능성이 있으므로 boundedElastic에서 1회만 수행
        Mono<String> jsonMono = Mono.fromCallable(() -> objectMapper.writeValueAsString(msg))
                .subscribeOn(Schedulers.boundedElastic())
                .cache(); // 아래에서 여러 번 구독할 때 재사용

        // 2) 저장: 실패해도 방송은 진행할지/중단할지 정책 선택 (여기선 "방송은 진행")
        Mono persistMono = switch (msg.messageType()) {
            case POST_COMMENT -> postCommentUseCase.save(msg);
            default -> globalChatUseCase.save(msg);
        };

        // 3) 방송: 느린/끊긴 세션이 전체를 막지 않도록 각 세션별 timeout + 에러시 제거
        Flux<Void> sendFlux = jsonMono.flatMapMany(json ->
                Flux.fromIterable(sessions)
                        .filter(WebSocketSession::isOpen)
                        // 너무 느린 세션이 전체를 막지 않게 동시성 제한
                        .flatMap(session ->
                                        session.send(Mono.just(session.textMessage(json)))
                                                .timeout(Duration.ofSeconds(3)) // 세션별 전송 제한
                                                .onErrorResume(e -> {
                                                    // 전송 실패/타임아웃 시 세션 정리
                                                    logger.debug("Send failed. Removing session: {} reason={}", session.getId(), e.toString());
                                                    sessions.remove(session);
                                                    try { session.close(); } catch (Exception ignore) {}
                                                    return Mono.empty();
                                                })
                                ,
                                64 /* concurrency 동시성 상한 설정(트래픽 보고 조정) */
                        )
        );

        // 4) "저장 후 방송" 보장 (저장이 실패해도 방송은 진행하도록 위에서 흡수)
        return persistMono.thenMany(sendFlux).then();
    }

    private Mono<Void> handleError(WebSocketSession session, Throwable ex) {
        log.error("WebSocket error: {}", ex.getMessage(), ex);
        // 필요하면 에러 응답 전송
        return Mono.empty();
    }
}
