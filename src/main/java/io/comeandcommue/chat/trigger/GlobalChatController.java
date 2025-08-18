package io.comeandcommue.chat.trigger;

import io.comeandcommue.chat.application.GlobalChatUseCase;
import io.comeandcommue.chat.application.PostCommentUseCase;
import io.comeandcommue.chat.domain.chat.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class GlobalChatController {
    private final GlobalChatUseCase globalChatUseCase;
    private final PostCommentUseCase  postCommentUseCase;

    @GetMapping("/global/messages")
    public Mono<List<Message>> getGlobalMessages(
            @RequestParam(required = false) int page
    ) {
        return globalChatUseCase.page(page, 30);
    }

    @GetMapping("/post/{postId}/comments")
    public Mono<List<Message>> getPostComments(
            @RequestParam(required = false) int page,
            @PathVariable String postId
    ) {
        return postCommentUseCase.page(postId, page, 30);
    }
}
