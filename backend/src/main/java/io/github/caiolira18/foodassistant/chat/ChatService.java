package io.github.caiolira18.foodassistant.chat;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.github.caiolira18.foodassistant.assistant.agent.AgentOrchestrator;
import io.github.caiolira18.foodassistant.assistant.agent.AgentResult;
import io.github.caiolira18.foodassistant.assistant.agent.ToolEvents;
import io.github.caiolira18.foodassistant.assistant.llm.Message;
import io.github.caiolira18.foodassistant.shared.SessionIds;

@Service
public class ChatService {

    static final int MAX_MESSAGE_LENGTH = 500;
    static final int MAX_CONCURRENT_TURNS = 8;
    static final int HISTORY_MESSAGES = 20;
    static final String EMPTY_REPLY = "Desculpe, não consegui responder.";

    public record Turn(String sessionId, String message) {
    }

    private final AgentOrchestrator orchestrator;
    private final ChatHistoryRepository history;
    private final Set<String> activeSessions = ConcurrentHashMap.newKeySet();
    private final Semaphore slots = new Semaphore(MAX_CONCURRENT_TURNS);

    ChatService(AgentOrchestrator orchestrator, ChatHistoryRepository history) {
        this.orchestrator = orchestrator;
        this.history = history;
    }

    /** Valida a entrada e reserva a sessão e uma vaga. Quem reservar precisa chamar release. */
    public Turn reserve(String sessionId, String message) {
        String session = SessionIds.validate(sessionId);
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("A mensagem não pode ser vazia");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("A mensagem pode ter no máximo " + MAX_MESSAGE_LENGTH + " caracteres");
        }
        if (!activeSessions.add(session)) {
            throw new ChatRejectedException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde a resposta anterior antes de enviar outra mensagem");
        }
        if (!slots.tryAcquire()) {
            activeSessions.remove(session);
            throw new ChatRejectedException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Muitas conversas ao mesmo tempo. Tente de novo em instantes");
        }
        return new Turn(session, message.strip());
    }

    public void release(Turn turn) {
        slots.release();
        activeSessions.remove(turn.sessionId());
    }

    public AgentResult run(Turn turn, Consumer<String> onToken, ToolEvents events) {
        List<Message> past = history.recent(turn.sessionId(), HISTORY_MESSAGES);
        AgentResult result = orchestrator.run(turn.sessionId(), past, turn.message(), onToken, events);
        String reply = result.reply().isBlank() ? EMPTY_REPLY : result.reply();
        history.appendTurn(turn.sessionId(), turn.message(), reply);
        return result;
    }
}