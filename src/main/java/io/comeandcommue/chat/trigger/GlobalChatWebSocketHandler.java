package io.comeandcommue.chat.trigger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.comeandcommue.chat.application.GlobalChatUseCase;
import io.comeandcommue.chat.domain.chat.ChatMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalChatWebSocketHandler implements WebSocketHandler {
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
                .flatMap(message -> {
                    log.debug("Received: {}", message);

                    // 메시지 파싱
                    ChatMessage chatMessage;
                    try {
                        Map<String, String> chatMessageMap = objectMapper.readValue(message, new TypeReference<>() {});
                        chatMessage = ChatMessage.of()
                                .sender(chatMessageMap.get("sender"))
                                .content(chatMessageMap.get("content"))
                                .ip(getClientIp(session))
                                .timestamp(System.currentTimeMillis())
                                .build();
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("메시지 파싱 실패", e));
                    }

                    // 메시지를 JSON 문자열로 직렬화
                    return Mono.fromCallable(() -> objectMapper.writeValueAsString(chatMessage))
                            .flatMapMany(json ->
                                    globalChatUseCase.save(chatMessage)
                                            .thenMany(
                                                    Flux.fromIterable(sessions)
                                                            .filter(WebSocketSession::isOpen)
                                                            .flatMap(s -> s.send(Mono.just(s.textMessage(json))))
                                            )
                            );
                })
                .then()
                .doFinally(signal -> {
                    sessions.remove(session);
                    log.debug("Client disconnected: {}", session.getId());
                });
    }

    private String getClientIp(WebSocketSession session) {
        return Optional.ofNullable(session.getHandshakeInfo().getRemoteAddress())
                .map(address -> address.getAddress().getHostAddress())
                .orElse("unknown");
    }
}
