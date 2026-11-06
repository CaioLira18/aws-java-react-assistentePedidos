package io.github.caiolira18.foodassistant.assistant.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.agent.AgentOrchestrator;
import io.github.caiolira18.foodassistant.assistant.agent.AgentOutcome;
import io.github.caiolira18.foodassistant.assistant.agent.AgentResult;
import io.github.caiolira18.foodassistant.assistant.agent.AgentSettings;
import io.github.caiolira18.foodassistant.assistant.agent.ToolRegistry;
import io.github.caiolira18.foodassistant.assistant.llm.FakeLlmClient;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchResult;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchService;

class CatalogToolsFlowTest {

    private final DishSearchService service = mock(DishSearchService.class);
    private final ToolRegistry registry = new ToolRegistry(
            List.of(new SearchDishesTool(service), new GetDishTool(service)));

    @Test
    void agentSearchesTheCatalogAndAnswersWithToolData() {
        when(service.search(anyString(), any(), anyInt())).thenReturn(List.of(
                new DishSearchResult(7, "Feijoada Vegana", "Feijão preto com legumes.", 3190, 45,
                        List.of("vegano"), 0.9)));
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "search_dishes",
                        "{\"query\":\"feijoada vegana\",\"max_price_cents\":4000}"))
                .enqueue(FakeLlmClient.text("Encontrei a Feijoada Vegana por R$ 31,90."));
        var orchestrator = new AgentOrchestrator(llm, registry, AgentSettings.defaults());

        AgentResult result = orchestrator.run("s1", List.of(), "quero feijoada vegana até 40 reais", null);

        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        Set<String> exposed = llm.received().get(0).tools().stream()
                .map(ToolDefinition::name).collect(Collectors.toSet());
        assertEquals(Set.of("search_dishes", "get_dish"), exposed);
        String toolMessage = llm.received().get(1).messages().get(2).text();
        assertTrue(toolMessage.contains("Feijoada Vegana"));
        assertTrue(toolMessage.contains("R$ 31,90"));
    }

    @Test
    void invalidToolArgumentsComeBackAsAnErrorAndTheAgentRecovers() {
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "search_dishes", "{\"max_price_cents\":4000}"))
                .enqueue(FakeLlmClient.toolCall("t2", "search_dishes", "{\"query\":\"feijoada\"}"))
                .enqueue(FakeLlmClient.text("Não achei nada com esses filtros."));
        var orchestrator = new AgentOrchestrator(llm, registry, AgentSettings.defaults());

        AgentResult result = orchestrator.run("s1", List.of(), "quero algo barato", null);

        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        assertTrue(llm.received().get(1).messages().get(2).text().contains("query"));
        verify(service, times(1)).search(eq("feijoada"), any(), eq(SearchDishesTool.DEFAULT_LIMIT));
    }
}