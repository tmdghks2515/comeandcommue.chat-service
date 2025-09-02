package io.comeandcommue.chat.trigger;

import io.comeandcommue.chat.application.usecase.GlobalChatUseCase;
import io.comeandcommue.chat.application.usecase.PostCommentUseCase;
import io.comeandcommue.chat.domain.chat.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class GlobalMessageController {
    private final GlobalChatUseCase globalChatUseCase;
    private final PostCommentUseCase  postCommentUseCase;

    @GetMapping("/global/messages")
    public Flux<Message> getGlobalMessages(
            @RequestParam(required = false) int page
    ) {
        return globalChatUseCase.page(page, 30);
    }

    @GetMapping("/post/{postId}/comments")
    public Flux<Message> getPostComments(
            @RequestParam(required = false) int page,
            @PathVariable String postId
    ) {
        return postCommentUseCase.page(postId, page, 30);
    }
}
