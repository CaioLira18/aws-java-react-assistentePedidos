package io.github.caiolira18.foodassistant.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.github.caiolira18.foodassistant.assistant.agent.AgentOrchestrator;
import io.github.caiolira18.foodassistant.assistant.agent.AgentOutcome;
import io.github.caiolira18.foodassistant.assistant.agent.AgentResult;
import io.github.caiolira18.foodassistant.assistant.llm.Message;
import io.github.caiolira18.foodassistant.assistant.llm.Usage;

class ChatServiceTest {

    private static final String SESSION = "sessao-12345678";

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final ChatHistoryRepository history = mock(ChatHistoryRepository.class);
    private final ChatService service = new ChatService(orchestrator, history);

    private static AgentResult result(String reply) {
        return new AgentResult(reply, AgentOutcome.COMPLETED, 1, new Usage(1, 1), Duration.ZERO, List.of());
    }

    @Test
    void rejectsInvalidInputWithoutReservingAnything() {
        assertThrows(IllegalArgumentException.class, () -> service.reserve("curto", "oi"));
        assertThrows(IllegalArgumentException.class, () -> service.reserve(SESSION, "  "));
        assertThrows(IllegalArgumentException.class, () -> service.reserve(SESSION, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.reserve(SESSION, "a".repeat(ChatService.MAX_MESSAGE_LENGTH + 1)));

        assertEquals("oi", service.reserve(SESSION, "oi").message());
    }

    @Test
    void onlyOneTurnPerSessionAtATime() {
        ChatService.Turn first = service.reserve(SESSION, "oi");

        ChatRejectedException e = assertThrows(ChatRejectedException.class, () -> service.reserve(SESSION, "de novo"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.status());

        service.release(first);
        assertDoesNotThrow(() -> service.reserve(SESSION, "agora vai"));
    }

    @Test
    void rejectsWhenAllSlotsAreBusyWithoutLeavingTheSessionStuck() {
        List<ChatService.Turn> turns = new ArrayList<>();
        for (int i = 0; i < ChatService.MAX_CONCURRENT_TURNS; i++) {
            turns.add(service.reserve("sessao-0000000" + i, "oi"));
        }

        ChatRejectedException e = assertThrows(ChatRejectedException.class, () -> service.reserve(SESSION, "oi"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.status());

        service.release(turns.get(0));
        assertDoesNotThrow(() -> service.reserve(SESSION, "oi"));
    }

    @Test
    void runLoadsHistoryCallsTheAgentAndPersistsTheTurn() {
        List<Message> past = List.of(Message.user("antes"), Message.assistant("resposta", List.of()));
        when(history.recent(SESSION, ChatService.HISTORY_MESSAGES)).thenReturn(past);
        when(orchestrator.run(eq(SESSION), eq(past), eq("oi"), any())).thenReturn(result("Olá!"));
        ChatService.Turn turn = service.reserve(SESSION, "  oi  ");

        AgentResult result = service.run(turn, token -> { });

        assertEquals("Olá!", result.reply());
        verify(history).appendTurn(SESSION, "oi", "Olá!");
    }
}