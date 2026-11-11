package io.github.caiolira18.foodassistant.assistant.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.agent.AgentOrchestrator;
import io.github.caiolira18.foodassistant.assistant.agent.AgentOutcome;
import io.github.caiolira18.foodassistant.assistant.agent.AgentResult;
import io.github.caiolira18.foodassistant.assistant.agent.AgentSettings;
import io.github.caiolira18.foodassistant.assistant.agent.ToolRegistry;
import io.github.caiolira18.foodassistant.assistant.llm.FakeLlmClient;
import io.github.caiolira18.foodassistant.ordering.CartLine;
import io.github.caiolira18.foodassistant.ordering.CartService;
import io.github.caiolira18.foodassistant.ordering.CartView;

class CartToolsFlowTest {

    @Test
    void agentAddsToTheCartOfTheCurrentSession() {
        CartService cart = mock(CartService.class);
        when(cart.addItem("sessao-12345678", 7L, 2)).thenReturn(
                new CartView(List.of(new CartLine(7, "Feijoada Vegana", 3190, 2, 6380)), 2, 6380));
        var registry = new ToolRegistry(List.of(new AddToCartTool(cart), new ViewCartTool(cart)));
        var llm = new FakeLlmClient()
                .enqueue(FakeLlmClient.toolCall("t1", "add_to_cart", "{\"dish_id\":7,\"quantity\":2}"))
                .enqueue(FakeLlmClient.text("Adicionei 2 Feijoadas Veganas. Total: R$ 63,80."));
        var orchestrator = new AgentOrchestrator(llm, registry, AgentSettings.defaults());

        AgentResult result = orchestrator.run("sessao-12345678", List.of(), "adicione 2 feijoadas veganas", null, null);

        assertEquals(AgentOutcome.COMPLETED, result.outcome());
        verify(cart).addItem("sessao-12345678", 7L, 2);
        assertTrue(llm.received().get(1).messages().get(2).text().contains("R$ 63,80"));
    }
}