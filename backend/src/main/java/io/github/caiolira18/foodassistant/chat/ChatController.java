package io.github.caiolira18.foodassistant.chat;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.caiolira18.foodassistant.assistant.agent.AgentResult;

@RestController
@RequestMapping("/api/chat")
class ChatController {

    static final String SESSION_HEADER = "X-Session-Id";
    static final long SSE_TIMEOUT_MS = 60_000L;
    static final String GENERIC_ERROR = "O assistente está indisponível no momento. Tente novamente.";

    record ChatRequest(String message) {
    }

    record TokenEvent(String text) {
    }

    record DoneEvent(String outcome, int iterations) {
    }

    record ErrorEvent(String message) {
    }

    private static class ClientGoneException extends RuntimeException {
        ClientGoneException(Throwable cause) {
            super(cause);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chat;
    private final Executor executor;

    ChatController(ChatService chat, @Qualifier("chatExecutor") Executor executor) {
        this.chat = chat;
        this.executor = executor;
    }

    @PostMapping
    ResponseEntity<SseEmitter> chat(@RequestHeader(SESSION_HEADER) String sessionId,
            @RequestBody ChatRequest body) {
        ChatService.Turn turn = chat.reserve(sessionId, body.message());
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        try {
            executor.execute(() -> stream(turn, emitter));
        } catch (RuntimeException e) {
            chat.release(turn);
            throw e;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    private void stream(ChatService.Turn turn, SseEmitter emitter) {
        try {
            AgentResult result = chat.run(turn, token -> send(emitter, "token", new TokenEvent(token)));
            send(emitter, "done", new DoneEvent(result.outcome().name(), result.iterations()));
            emitter.complete();
        } catch (ClientGoneException e) {
            log.debug("Cliente desconectou durante o turno");
        } catch (RuntimeException e) {
            log.error("Falha no turno do chat", e);
            try {
                send(emitter, "error", new ErrorEvent(GENERIC_ERROR));
                emitter.complete();
            } catch (ClientGoneException ignored) {
                // cliente já foi embora
            }
        } finally {
            chat.release(turn);
        }
    }

    private static void send(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            throw new ClientGoneException(e);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ChatRejectedException.class)
    ResponseEntity<Map<String, String>> rejected(ChatRejectedException e) {
        return ResponseEntity.status(e.status()).contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("error", e.getMessage()));
    }
}