package io.github.caiolira18.foodassistant.chat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.caiolira18.foodassistant.assistant.agent.AgentOrchestrator;
import io.github.caiolira18.foodassistant.assistant.agent.AgentOutcome;
import io.github.caiolira18.foodassistant.assistant.agent.AgentResult;
import io.github.caiolira18.foodassistant.assistant.agent.DishSummary;
import io.github.caiolira18.foodassistant.assistant.agent.ToolEvents;
import io.github.caiolira18.foodassistant.assistant.llm.Usage;

class ChatControllerTest {

    private static final String SESSION = "sessao-12345678";

    private final AgentOrchestrator orchestrator = mock(AgentOrchestrator.class);
    private final ChatHistoryRepository history = mock(ChatHistoryRepository.class);
    private final ChatService service = new ChatService(orchestrator, history);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ChatController(service, Runnable::run)).build();

    private String streamBody(String json) throws Exception {
        MvcResult started = mvc.perform(post("/api/chat").header("X-Session-Id", SESSION)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(started)).andExpect(status().isOk());
        return started.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void streamsTokensAndFinishesWithADoneEvent() throws Exception {
        when(orchestrator.run(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            onToken.accept("Ola");
            onToken.accept(" mundo");
            return new AgentResult("Ola mundo", AgentOutcome.COMPLETED, 1, new Usage(1, 1), Duration.ZERO, List.of());
        });

        String body = streamBody("{\"message\":\"oi\"}");

        assertTrue(body.contains("event:token"));
        assertTrue(body.contains("{\"text\":\"Ola\"}"));
        assertTrue(body.contains("{\"text\":\" mundo\"}"));
        assertTrue(body.contains("event:done"));
        assertTrue(body.contains("\"outcome\":\"COMPLETED\""));
        verify(history).appendTurn(SESSION, "oi", "Ola mundo");
    }

    @Test
    void agentFailureBecomesAGenericErrorEventAndFreesTheSession() throws Exception {
        when(orchestrator.run(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("segredo interno"));

        String body = streamBody("{\"message\":\"oi\"}");

        assertTrue(body.contains("event:error"));
        assertFalse(body.contains("segredo"));
        assertDoesNotThrow(() -> service.reserve(SESSION, "outra"));
    }

    @Test
    void invalidSessionIsABadRequest() throws Exception {
        mvc.perform(post("/api/chat").header("X-Session-Id", "x")
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"oi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Sessão inválida"));
    }

    @Test
    void blankMessageIsABadRequest() throws Exception {
        mvc.perform(post("/api/chat").header("X-Session-Id", SESSION)
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A mensagem não pode ser vazia"));
    }

    @Test
    void busySessionGets429() throws Exception {
        service.reserve(SESSION, "em andamento");

        mvc.perform(post("/api/chat").header("X-Session-Id", SESSION)
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"oi\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void emitsTheDishesFoundByTheAgent() throws Exception {
        when(orchestrator.run(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            ToolEvents events = invocation.getArgument(4);
            events.dishesFound(
                    List.of(new DishSummary(7, "Feijoada Vegana", "Feijão preto.", 3190, 45, List.of("vegano"))));
            return new AgentResult("Achei!", AgentOutcome.COMPLETED, 1, new Usage(1, 1), Duration.ZERO, List.of());
        });

        String body = streamBody("{\"message\":\"oi\"}");

        assertTrue(body.contains("event:dishes"));
        assertTrue(body.contains("\"priceCents\":3190"));
        assertTrue(body.contains("Feijoada Vegana"));
    }
}