package io.github.caiolira18.foodassistant.assistant.agent;

import static io.github.caiolira18.foodassistant.assistant.agent.ToolRegistryTest.tool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.llm.FakeLlmClient;
import io.github.caiolira18.foodassistant.assistant.llm.LlmRequest;
import io.github.caiolira18.foodassistant.assistant.llm.LlmResponse;
import io.github.caiolira18.foodassistant.assistant.llm.Message;
import io.github.caiolira18.foodassistant.assistant.llm.ToolCall;
import io.github.caiolira18.foodassistant.assistant.llm.Usage;

class AgentOrchestratorTest {

    private static ToolRegistry registryWith(AgentTool... tools) {
        return new ToolRegistry(List.of(tools));
    }

    private static AgentTool searchTool() {
        return tool("search_dishes", (args, ctx) -> "[{\"name\":\"Feijoada Vegana\"}]");
    }

    @Test
    void answersDirectlyWithoutTools() {
        var llm = new FakeLlmClient().enqueue(FakeLlmClient.text("Olá! Como posso ajudar?"));
        var orchestrator = new AgentOrchestrator(llm, registryWith(searchTool()), AgentSettings.defaults());

        AgentResult result = orchestrator.run("s1", List.of(), "oi", null, ToolEvents.NONE);

        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        assertEquals("Olá! Como posso ajudar?", result.reply());
        assertEquals(1, result.iterations());
        assertEquals(2, result.newMessages().size());
        LlmRequest sent = llm.received().get(0);
        assertEquals(AgentPrompt.SYSTEM, sent.systemPrompt());
        assertEquals(1, sent.tools().size());
        assertEquals(AgentSettings.defaults().maxTokens(), sent.maxTokens());
    }

    @Test
    void executesToolAndFeedsTheResultBackToTheModel() {
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "search_dishes", "{\"query\":\"vegano\"}"))
                .enqueue(FakeLlmClient.text("Achei a Feijoada Vegana."));
        List<String> seen = new ArrayList<>();
        var registry = registryWith(tool("search_dishes", (args, ctx) -> {
            seen.add(args + "|" + ctx.sessionId());
            return "[{\"name\":\"Feijoada Vegana\"}]";
        }));
        var orchestrator = new AgentOrchestrator(llm, registry, AgentSettings.defaults());

        AgentResult result = orchestrator.run("s1", List.of(), "quero algo vegano", null, ToolEvents.NONE);

        assertEquals(List.of("{\"query\":\"vegano\"}|s1"), seen);
        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        assertEquals(2, result.iterations());
        assertEquals(new Usage(20, 10), result.usage());
        List<Message> second = llm.received().get(1).messages();
        assertEquals(3, second.size());
        assertEquals(Message.Role.ASSISTANT, second.get(1).role());
        assertEquals("t1", second.get(1).toolCalls().get(0).id());
        assertEquals(Message.Role.TOOL, second.get(2).role());
        assertEquals("t1", second.get(2).toolCallId());
        assertEquals(4, result.newMessages().size());
    }

    @Test
    void toolValidationErrorIsReturnedToTheModelAndTheLoopContinues() {
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "search_dishes", "{}"))
                .enqueue(FakeLlmClient.text("Preciso saber o que você procura."));
        var registry = registryWith(tool("search_dishes", (args, ctx) -> {
            throw new ToolException("query é obrigatória");
        }));
        var orchestrator = new AgentOrchestrator(llm, registry, AgentSettings.defaults());

        AgentResult result = orchestrator.run("s1", List.of(), "quero comer", null, ToolEvents.NONE);

        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        Message toolMessage = llm.received().get(1).messages().get(2);
        assertTrue(toolMessage.text().contains("query é obrigatória"));
    }

    @Test
    void unknownToolIsReportedBackToTheModel() {
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "drop_database", "{}"))
                .enqueue(FakeLlmClient.text("Não consegui fazer isso."));
        var orchestrator = new AgentOrchestrator(llm, registryWith(searchTool()), AgentSettings.defaults());

        AgentResult result = orchestrator.run("s1", List.of(), "apague tudo", null, ToolEvents.NONE);

        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        assertTrue(llm.received().get(1).messages().get(2).text().contains("desconhecida"));
    }

    @Test
    void stopsAtTheIterationLimitWithAFallbackReply() {
        var settings = new AgentSettings(3, Duration.ofSeconds(30), 512, 20, 4);
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "search_dishes", "{}"))
                .enqueue(FakeLlmClient.toolCall("t2", "search_dishes", "{}"))
                .enqueue(FakeLlmClient.toolCall("t3", "search_dishes", "{}"));
        var orchestrator = new AgentOrchestrator(llm, registryWith(searchTool()), settings);
        List<String> tokens = new ArrayList<>();

        AgentResult result = orchestrator.run("s1", List.of(), "busque", tokens::add, ToolEvents.NONE);

        assertEquals(AgentOutcome.ITERATION_LIMIT, result.outcome());
        assertEquals(3, llm.received().size());
        assertEquals(AgentOrchestrator.ITERATION_LIMIT_REPLY, result.reply());
        assertEquals(List.of(AgentOrchestrator.ITERATION_LIMIT_REPLY), tokens);
        assertEquals(Message.Role.ASSISTANT, result.newMessages().get(result.newMessages().size() - 1).role());
    }

    @Test
    void stopsWhenTheTimeoutIsExceeded() {
        var clock = new MutableClock();
        var llm = new FakeLlmClient() {
            @Override
            public LlmResponse generate(LlmRequest request) {
                LlmResponse response = super.generate(request);
                clock.advance(Duration.ofSeconds(31));
                return response;
            }
        }.enqueue(FakeLlmClient.toolCall("t1", "search_dishes", "{}"))
                .enqueue(FakeLlmClient.text("nunca deve chegar aqui"));
        var settings = new AgentSettings(5, Duration.ofSeconds(30), 512, 20, 4);
        var orchestrator = new AgentOrchestrator(llm, registryWith(searchTool()), settings, clock);

        AgentResult result = orchestrator.run("s1", List.of(), "busque", null, ToolEvents.NONE);

        assertEquals(AgentOutcome.TIMEOUT, result.outcome());
        assertEquals(1, llm.received().size());
        assertEquals(AgentOrchestrator.TIMEOUT_REPLY, result.reply());
    }

    @Test
    void streamsTheFinalTextToTheCaller() {
        var llm = new FakeLlmClient().enqueue(FakeLlmClient.text("Olá!"));
        var orchestrator = new AgentOrchestrator(llm, registryWith(searchTool()), AgentSettings.defaults());
        List<String> tokens = new ArrayList<>();

        orchestrator.run("s1", List.of(), "oi", tokens::add, ToolEvents.NONE);

        assertEquals(List.of("Olá!"), tokens);
    }

    @Test
    void trimsHistoryWithoutSplittingToolPairs() {
        List<Message> history = List.of(
                Message.user("u1"),
                Message.assistant("a1", List.of()),
                Message.user("u2"),
                Message.assistant("", List.of(new ToolCall("t1", "search_dishes", "{}"))),
                Message.toolResult("t1", "[]"),
                Message.assistant("a2", List.of()));

        var llmSmall = new FakeLlmClient().enqueue(FakeLlmClient.text("ok"));
        new AgentOrchestrator(llmSmall, registryWith(searchTool()),
                new AgentSettings(5, Duration.ofSeconds(30), 512, 3, 4))
                .run("s1", history, "novo", null, ToolEvents.NONE);
        List<Message> sentSmall = llmSmall.received().get(0).messages();
        assertEquals(1, sentSmall.size());
        assertEquals("novo", sentSmall.get(0).text());

        var llmBig = new FakeLlmClient().enqueue(FakeLlmClient.text("ok"));
        new AgentOrchestrator(llmBig, registryWith(searchTool()),
                new AgentSettings(5, Duration.ofSeconds(30), 512, 4, 4))
                .run("s1", history, "novo", null, ToolEvents.NONE);
        List<Message> sentBig = llmBig.received().get(0).messages();
        assertEquals(5, sentBig.size());
        assertEquals("u2", sentBig.get(0).text());
    }

    @Test
    void answersEveryToolCallEvenWhenOverThePerTurnLimit() {
        var three = new LlmResponse("", List.of(
                new ToolCall("a", "search_dishes", "{}"),
                new ToolCall("b", "search_dishes", "{}"),
                new ToolCall("c", "search_dishes", "{}")),
                new Usage(1, 1), LlmResponse.StopReason.TOOL_USE);
        var llm = new FakeLlmClient().enqueue(three).enqueue(FakeLlmClient.text("pronto"));
        AtomicInteger executed = new AtomicInteger();
        var registry = registryWith(tool("search_dishes", (args, ctx) -> {
            executed.incrementAndGet();
            return "[]";
        }));
        var settings = new AgentSettings(5, Duration.ofSeconds(30), 512, 20, 2);

        new AgentOrchestrator(llm, registry, settings).run("s1", List.of(), "busque", null, ToolEvents.NONE);

        assertEquals(2, executed.get());
        List<Message> second = llm.received().get(1).messages();
        assertEquals(5, second.size());
        assertEquals("c", second.get(4).toolCallId());
        assertTrue(second.get(4).text().contains("Limite"));
    }

    @Test
    void reportsMaxTokensWhenTheModelIsCutOff() {
        var cut = new LlmResponse("Texto cortado", List.of(), new Usage(5, 5), LlmResponse.StopReason.MAX_TOKENS);
        var llm = new FakeLlmClient().enqueue(cut);
        var orchestrator = new AgentOrchestrator(llm, registryWith(searchTool()), AgentSettings.defaults());

        AgentResult result = orchestrator.run("s1", List.of(), "conte uma história", null, ToolEvents.NONE);

        assertEquals(AgentOutcome.MAX_TOKENS, result.outcome());
        assertEquals("Texto cortado", result.reply());
    }

    @Test
    void rejectsBlankSessionAndBlankMessage() {
        var orchestrator = new AgentOrchestrator(new FakeLlmClient(), registryWith(searchTool()),
                AgentSettings.defaults());

        assertThrows(IllegalArgumentException.class, () -> orchestrator.run(" ", List.of(), "oi", null, ToolEvents.NONE));
        assertThrows(IllegalArgumentException.class, () -> orchestrator.run("s1", List.of(), "  ", null, ToolEvents.NONE));
    }

    private static class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void toolsCanPublishEventsGivenToTheRun() {
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "search_dishes", "{}"))
                .enqueue(FakeLlmClient.text("ok"));
        var registry = registryWith(tool("search_dishes", (args, ctx) -> {
            ctx.events().dishesFound(List.of(new DishSummary(1, "X", "d", 1000, 10, List.of())));
            return "[]";
        }));
        List<DishSummary> published = new ArrayList<>();

        new AgentOrchestrator(llm, registry, AgentSettings.defaults())
                .run("s1", List.of(), "busque", null, published::addAll);

        assertEquals(1, published.size());
    }
}