package io.comeandcommue.chat.trigger;

import io.comeandcommue.chat.application.GlobalChatUseCase;
import io.comeandcommue.chat.domain.chat.Message;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalChatWebSocketHandler implements WebSocketHandler {
    private final GlobalChatUseCase globalChatUseCase;

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public @NonNull Mono<Void> handle(@NonNull WebSocketSession session) {
        sessions.add(session);
        log.debug("Client connected: {}", session.getId());
        session.getAttributes().put("nickname", "종한어");

        return session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(msg -> log.debug("Received: {}", msg))
                .flatMap(message ->
                        // 메시지를 Redis에 저장 + 모든 세션에 브로드캐스트
                        globalChatUseCase.save(
                                Message.of()
                                        .sender((String) session.getAttributes().get("nickname"))
                                        .content(message)
                                        .ip(getClientIp(session))
                                        .timestamp(System.currentTimeMillis())
                                        .build()
                        ).thenMany(
                                Flux.fromIterable(sessions)
                                        .filter(WebSocketSession::isOpen)
                                        .flatMap(s -> s.send(Mono.just(s.textMessage(message))))
                        )
                )
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
