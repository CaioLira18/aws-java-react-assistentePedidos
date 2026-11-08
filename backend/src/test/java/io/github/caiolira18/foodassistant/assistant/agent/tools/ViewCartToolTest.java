package io.github.caiolira18.foodassistant.assistant.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.ordering.CartService;
import io.github.caiolira18.foodassistant.ordering.CartView;
import tools.jackson.databind.json.JsonMapper;

class ViewCartToolTest {

    private static final String SESSION = "sessao-12345678";
    private static final ToolContext CONTEXT = new ToolContext(SESSION);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final CartService cart = mock(CartService.class);
    private final ViewCartTool tool = new ViewCartTool(cart);

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) {
        return (Map<String, Object>) MAPPER.readValue(json, Map.class);
    }

    @Test
    void emptyCartIsReportedAsEmpty() {
        when(cart.view(SESSION)).thenReturn(CartView.empty());

        Map<String, Object> json = parse(tool.execute("{}", CONTEXT));

        assertEquals(true, json.get("is_empty"));
        assertEquals(List.of(), json.get("items"));
        assertEquals("R$ 0,00", json.get("total_brl"));
    }

    @Test
    void acceptsBlankArgumentsAndAlwaysUsesTheContextSession() {
        when(cart.view(SESSION)).thenReturn(CartView.empty());

        tool.execute("", CONTEXT);
        tool.execute("{}", CONTEXT);

        verify(cart, times(2)).view(SESSION);
        verifyNoMoreInteractions(cart);
    }

    @Test
    void rejectsAnyArgument() {
        ToolException e = assertThrows(ToolException.class,
                () -> tool.execute("{\"session_id\":\"outra-sessao-123\"}", CONTEXT));

        assertTrue(e.getMessage().contains("Campo desconhecido"));
        verifyNoMoreInteractions(cart);
    }
}