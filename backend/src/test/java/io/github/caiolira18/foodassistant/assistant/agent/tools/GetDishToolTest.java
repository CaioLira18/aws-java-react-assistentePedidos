package io.github.caiolira18.foodassistant.assistant.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.catalog.search.DishDetails;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchService;
import tools.jackson.databind.json.JsonMapper;

class GetDishToolTest {

    private static final ToolContext CONTEXT = new ToolContext("s1");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final DishSearchService service = mock(DishSearchService.class);
    private final GetDishTool tool = new GetDishTool(service);

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
        assertEquals("get_dish", tool.definition().name());
        assertEquals(List.of("dish_id"), parse(tool.definition().inputSchemaJson()).get("required"));
    }

    @Test
    void returnsDishDetailsFromTheCatalog() {
        when(service.getDish(7)).thenReturn(Optional.of(
                new DishDetails(7, "Feijoada Vegana", "Feijão preto com legumes.", 3190, 45,
                        List.of("vegano", "sem-gluten"))));

        Map<String, Object> json = parse(tool.execute("{\"dish_id\":7}", CONTEXT));

        assertEquals(7, json.get("id"));
        assertEquals("Feijoada Vegana", json.get("name"));
        assertEquals("R$ 31,90", json.get("price_brl"));
        assertEquals(45, json.get("prep_minutes"));
        assertEquals(List.of("vegano", "sem-gluten"), json.get("dietary_tags"));
    }

    @Test
    void unknownDishBecomesAToolError() {
        ToolException e = assertThrows(ToolException.class, () -> tool.execute("{\"dish_id\":99}", CONTEXT));

        assertTrue(e.getMessage().contains("não encontrado"));
    }

    @Test
    void rejectsInvalidArgumentsWithoutTouchingTheCatalog() {
        assertInvalid("{}", "dish_id");
        assertInvalid("{\"dish_id\":\"7\"}", "dish_id");
        assertInvalid("{\"dish_id\":0}", "dish_id");
        assertInvalid("{\"dish_id\":1.5}", "dish_id");
        assertInvalid("{\"dish_id\":7,\"x\":1}", "Campo desconhecido");

        verifyNoInteractions(service);
    }
}