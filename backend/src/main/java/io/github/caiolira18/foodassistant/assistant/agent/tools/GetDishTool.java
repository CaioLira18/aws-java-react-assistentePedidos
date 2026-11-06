package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.caiolira18.foodassistant.assistant.agent.AgentTool;
import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;
import io.github.caiolira18.foodassistant.catalog.search.DishDetails;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchService;

@Component
class GetDishTool implements AgentTool {

    static final String NAME = "get_dish";

    private static final List<String> FIELDS = List.of("dish_id");

    private static final String DESCRIPTION = """
            Retorna os detalhes de um prato pelo id (obtido em search_dishes). \
            Use para confirmar preço, tempo de preparo e restrições antes de falar deles.""";

    private static final String SCHEMA = """
            {
              "type": "object",
              "properties": {
                "dish_id": {"type": "integer", "minimum": 1, "description": "Id do prato, obtido na busca."}
              },
              "required": ["dish_id"]
            }""";

    private static final ToolDefinition DEFINITION = new ToolDefinition(NAME, DESCRIPTION, SCHEMA);

    private final DishSearchService search;

    GetDishTool(DishSearchService search) {
        this.search = search;
    }

    @Override
    public ToolDefinition definition() {
        return DEFINITION;
    }

    @Override
    public String execute(String argumentsJson, ToolContext context) {
        ToolArguments args = ToolArguments.parse(argumentsJson, FIELDS);
        int id = args.requiredInt("dish_id", 1, Integer.MAX_VALUE);

        DishDetails dish = search.getDish(id)
                .orElseThrow(() -> new ToolException("Prato não encontrado ou indisponível"));

        return ToolJson.write(DishToolFormat.dishMap(
                dish.id(), dish.name(), dish.description(), dish.priceCents(), dish.prepMinutes(),
                dish.dietaryTags()));
    }
}