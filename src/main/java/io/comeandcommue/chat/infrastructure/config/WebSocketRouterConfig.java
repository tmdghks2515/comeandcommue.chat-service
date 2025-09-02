package io.comeandcommue.chat.infrastructure.config;

import io.comeandcommue.chat.trigger.GlobalMessageWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

@Configuration
public class WebSocketRouterConfig {

    @Bean
    public HandlerMapping webSocketMapping(GlobalMessageWebSocketHandler globalChatWebSocketHandler) {
        return new SimpleUrlHandlerMapping() {{
            setOrder(-1);
            setUrlMap(Map.of("/ws/chat", globalChatWebSocketHandler));
        }};
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
