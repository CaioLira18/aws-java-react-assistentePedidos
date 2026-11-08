package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiolira18.foodassistant.assistant.agent.AgentTool;
import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;
import io.github.caiolira18.foodassistant.ordering.CartService;

@Component
class ViewCartTool implements AgentTool {

    static final String NAME = "view_cart";

    private static final String DESCRIPTION = """
            Mostra o carrinho atual do cliente: itens, quantidades, preços e total. \
            Não recebe argumentos.""";

    private static final String SCHEMA = """
            {"type": "object", "properties": {}}""";

    private static final ToolDefinition DEFINITION = new ToolDefinition(NAME, DESCRIPTION, SCHEMA);

    private final CartService cart;

    ViewCartTool(CartService cart) {
        this.cart = cart;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public String execute(String argumentsJson, ToolContext context) {
        String json = argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson;
        ToolArguments.parse(json, List.of());
        try {
            return CartToolFormat.toJson(cart.view(context.sessionId()));
        } catch (IllegalArgumentException e) {
            throw new ToolException(e.getMessage());
        }
    }
}