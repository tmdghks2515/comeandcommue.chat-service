package io.comeandcommue.chat.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.application.GlobalChatUseCase;
import io.comeandcommue.chat.domain.chat.ChatMessage;
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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalChatWebSocketHandler implements WebSocketHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalChatWebSocketHandler.class);
    private final GlobalChatUseCase globalChatUseCase;

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final ObjectMapper objectMapper;

    @Override
    public @NonNull Mono<Void> handle(@NonNull WebSocketSession session) {
        sessions.add(session);
        log.debug("Client connected: {}", session.getId());
        session.getAttributes().put("nickname", "종한어");

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

    private ChatMessage parseAndValidate(String payload) throws JsonProcessingException {
        ChatMessage message = objectMapper.readValue(payload, ChatMessage.class);

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

    private Mono<Void> broadcast(ChatMessage chatMessage) {
        logger.debug("[GlobalChatWebSocketHandler] broadcast >> {}", chatMessage.toString());
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(chatMessage))
                .flatMapMany(json ->
                        globalChatUseCase.save(chatMessage)
                                .thenMany(
                                        Flux.fromIterable(sessions)
                                                .filter(WebSocketSession::isOpen)
                                                .flatMap(s -> s.send(Mono.just(s.textMessage(json))))
                                )
                )
                .then();
    }

    private Mono<Void> handleError(WebSocketSession session, Throwable ex) {
        log.error("WebSocket error: {}", ex.getMessage(), ex);
        // 필요하면 에러 응답 전송
        return Mono.empty();
    }
}
