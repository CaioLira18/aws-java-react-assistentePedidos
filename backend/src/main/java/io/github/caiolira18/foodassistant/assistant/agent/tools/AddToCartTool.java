package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiolira18.foodassistant.assistant.agent.AgentTool;
import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;
import io.github.caiolira18.foodassistant.ordering.CartService;

@Component
class AddToCartTool implements AgentTool {

    static final String NAME = "add_to_cart";

    private static final List<String> FIELDS = List.of("dish_id", "quantity");

    private static final String DESCRIPTION = """
            Adiciona um prato ao carrinho do cliente. Use somente quando o cliente pedir de forma explícita. \
            Se o prato já estiver no carrinho, a quantidade é somada (máximo de 10 por prato). \
            O preço vem sempre do catálogo. Retorna o carrinho atualizado.""";

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "dish_id": {"type": "integer", "minimum": 1, "description": "Id do prato, obtido na busca."},
                "quantity": {"type": "integer", "minimum": 1, "maximum": 10, "description": "Quantidade a adicionar. Padrão 1."}
              },
              "required": ["dish_id"]
            }""";

    private static final ToolDefinition DEFINITION = new ToolDefinition(NAME, DESCRIPTION, SCHEMA);

    private final CartService cart;

    AddToCartTool(CartService cart) {
        this.cart = cart;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public String execute(String argumentsJson, ToolContext context) {
        ToolArguments args = ToolArguments.parse(argumentsJson, FIELDS);
        int dishId = args.requiredInt("dish_id", 1, Integer.MAX_VALUE);
        Integer quantity = args.optionalInt("quantity", 1, CartService.MAX_QUANTITY_PER_ITEM);
        try {
            return CartToolFormat.toJson(
                    cart.addItem(context.sessionId(), (long) dishId, quantity == null ? 1 : quantity));
        } catch (IllegalArgumentException e) {
            throw new ToolException(e.getMessage());
        }
    }
}