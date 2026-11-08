package io.github.caiolira18.foodassistant.assistant.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.ordering.CartLine;
import io.github.caiolira18.foodassistant.ordering.CartService;
import io.github.caiolira18.foodassistant.ordering.CartView;
import tools.jackson.databind.json.JsonMapper;

class AddToCartToolTest {

    private static final String SESSION = "sessao-12345678";
    private static final ToolContext CONTEXT = new ToolContext(SESSION);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final CartService cart = mock(CartService.class);
    private final AddToCartTool tool = new AddToCartTool(cart);

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) {
        return (Map<String, Object>) MAPPER.readValue(json, Map.class);
    }

    private void assertInvalid(String json, String messagePart) {
        ToolException e = assertThrows(ToolException.class, () -> tool.execute(json, CONTEXT));
        assertTrue(e.getMessage().contains(messagePart), e.getMessage());
    }

    @Test
    void definitionHasAValidSchemaWithRequiredDishId() {
        assertEquals("add_to_cart", tool.definition().name());
        assertEquals(List.of("dish_id"), parse(tool.definition().inputSchemaJson()).get("required"));
    }

    @Test
    void usesTheSessionFromTheContextAndTheValidatedArguments() {
        when(cart.addItem(any(), any(), any())).thenReturn(CartView.empty());

        tool.execute("{\"dish_id\":7,\"quantity\":2}", CONTEXT);

        verify(cart).addItem(SESSION, 7L, 2);
    }

    @Test
    void quantityDefaultsToOne() {
        when(cart.addItem(any(), any(), any())).thenReturn(CartView.empty());

        tool.execute("{\"dish_id\":7}", CONTEXT);

        verify(cart).addItem(SESSION, 7L, 1);
    }

    @Test
    void returnsTheUpdatedCartWithBrazilianPrices() {
        when(cart.addItem(any(), any(), any())).thenReturn(
                new CartView(List.of(new CartLine(7, "Feijoada Vegana", 3190, 2, 6380)), 2, 6380));

        Map<String, Object> json = parse(tool.execute("{\"dish_id\":7,\"quantity\":2}", CONTEXT));

        assertEquals(false, json.get("is_empty"));
        assertEquals(2, json.get("total_items"));
        assertEquals("R$ 63,80", json.get("total_brl"));
        Map<?, ?> line = (Map<?, ?>) ((List<?>) json.get("items")).get(0);
        assertEquals("R$ 31,90", line.get("unit_price_brl"));
        assertEquals("R$ 63,80", line.get("line_total_brl"));
    }

    @Test
    void theModelCannotChooseTheSession() {
        assertInvalid("{\"dish_id\":7,\"session_id\":\"outra-sessao-123\"}", "Campo desconhecido");

        verifyNoInteractions(cart);
    }

    @Test
    void rejectsInvalidArguments() {
        assertInvalid("{}", "dish_id");
        assertInvalid("{\"dish_id\":\"7\"}", "dish_id");
        assertInvalid("{\"dish_id\":7,\"quantity\":0}", "quantity");
        assertInvalid("{\"dish_id\":7,\"quantity\":11}", "quantity");
        assertInvalid("{\"dish_id\":7,\"quantity\":1.5}", "quantity");

        verifyNoInteractions(cart);
    }

    @Test
    void cartRuleViolationsBecomeToolErrors() {
        when(cart.addItem(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Máximo de 10 unidades por item (já há 9 no carrinho)"));

        assertInvalid("{\"dish_id\":7,\"quantity\":5}", "Máximo de 10");
    }
}