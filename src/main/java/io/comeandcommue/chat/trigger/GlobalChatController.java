package io.comeandcommue.chat.trigger;

import io.comeandcommue.chat.application.GlobalChatUseCase;
import io.comeandcommue.chat.domain.chat.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/chat/global")
@RequiredArgsConstructor
public class GlobalChatController {
    private final GlobalChatUseCase globalChatUseCase;

    @GetMapping("/messages")
    public Mono<List<Message>> getMessages(
            @RequestParam(required = false) Long beforeTimestamp) {
        return globalChatUseCase.getMessages(beforeTimestamp);
    }
}
