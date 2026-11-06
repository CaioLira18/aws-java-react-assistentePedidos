package io.github.caiolira18.foodassistant.assistant.agent.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchFilters;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchResult;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchService;
import tools.jackson.databind.json.JsonMapper;

class SearchDishesToolTest {

    private static final ToolContext CONTEXT = new ToolContext("s1");
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final DishSearchService service = mock(DishSearchService.class);
    private final SearchDishesTool tool = new SearchDishesTool(service);

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String json) {
        return (Map<String, Object>) MAPPER.readValue(json, Map.class);
    }

    private static DishSearchResult result(long id, String name, String description, int price, int prep,
            double similarity) {
        return new DishSearchResult(id, name, description, price, prep, List.of("vegano"), similarity);
    }

    private ArgumentCaptor<DishSearchFilters> captureFilters(String expectedQuery, int expectedLimit) {
        ArgumentCaptor<DishSearchFilters> captor = ArgumentCaptor.forClass(DishSearchFilters.class);
        verify(service).search(eq(expectedQuery), captor.capture(), eq(expectedLimit));
        return captor;
    }

    private void assertInvalid(String json, String messagePart) {
        ToolException e = assertThrows(ToolException.class, () -> tool.execute(json, CONTEXT));
        assertTrue(e.getMessage().contains(messagePart), e.getMessage());
    }

    @Test
    void definitionHasAValidSchemaWithRequiredQuery() {
        ToolDefinition definition = tool.definition();

        assertEquals("search_dishes", definition.name());
        assertEquals(List.of("query"), parse(definition.inputSchemaJson()).get("required"));
    }

    @Test
    void passesValidatedArgumentsToTheService() {
        tool.execute("{\"query\":\"almoço vegano\",\"max_price_cents\":4000,\"max_prep_minutes\":30,"
                + "\"dietary_tags\":[\"vegano\",\"sem-gluten\"],\"limit\":3}", CONTEXT);

        DishSearchFilters filters = captureFilters("almoço vegano", 3).getValue();
        assertEquals(4000, filters.maxPriceCents());
        assertEquals(30, filters.maxPrepMinutes());
        assertEquals(Set.of("vegano", "sem-gluten"), filters.requiredTags());
    }

    @Test
    void usesDefaultsWhenOnlyTheQueryIsGiven() {
        tool.execute("{\"query\":\"algo leve\"}", CONTEXT);

        DishSearchFilters filters = captureFilters("algo leve", SearchDishesTool.DEFAULT_LIMIT).getValue();
        assertNull(filters.maxPriceCents());
        assertNull(filters.maxPrepMinutes());
        assertTrue(filters.requiredTags().isEmpty());
    }

    @Test
    void normalizesTagsWrittenLoosely() {
        tool.execute("{\"query\":\"x\",\"dietary_tags\":[\" Sem Glúten \",\"VEGANO\",\"vegano\"]}", CONTEXT);

        DishSearchFilters filters = captureFilters("x", SearchDishesTool.DEFAULT_LIMIT).getValue();
        assertEquals(Set.of("sem-gluten", "vegano"), filters.requiredTags());
    }

    @Test
    void formatsResultsWithBrazilianPriceAndRoundedSimilarity() {
        when(service.search(anyString(), any(), anyInt())).thenReturn(List.of(
                result(7, "Feijoada Vegana", "Feijão preto com legumes.", 3190, 45, 0.87654)));

        Map<String, Object> json = parse(tool.execute("{\"query\":\"feijoada\"}", CONTEXT));

        assertEquals(1, json.get("count"));
        Map<?, ?> first = (Map<?, ?>) ((List<?>) json.get("dishes")).get(0);
        assertEquals(7, first.get("id"));
        assertEquals("Feijoada Vegana", first.get("name"));
        assertEquals(3190, first.get("price_cents"));
        assertEquals("R$ 31,90", first.get("price_brl"));
        assertEquals(45, first.get("prep_minutes"));
        assertEquals(0.877, first.get("similarity"));
        assertEquals(List.of("vegano"), first.get("dietary_tags"));
    }

    @Test
    void truncatesLongDescriptions() {
        when(service.search(anyString(), any(), anyInt())).thenReturn(List.of(
                result(1, "Prato", "a".repeat(500), 1000, 10, 0.5)));

        Map<String, Object> json = parse(tool.execute("{\"query\":\"x\"}", CONTEXT));

        String description = (String) ((Map<?, ?>) ((List<?>) json.get("dishes")).get(0)).get("description");
        assertTrue(description.length() <= DishToolFormat.MAX_DESCRIPTION_LENGTH);
        assertTrue(description.endsWith("…"));
    }

    @Test
    void emptyResultsIncludeAHintForTheModel() {
        Map<String, Object> json = parse(tool.execute("{\"query\":\"nada\"}", CONTEXT));

        assertEquals(0, json.get("count"));
        assertTrue(json.containsKey("note"));
    }

    @Test
    void rejectsInvalidArgumentsWithoutTouchingTheCatalog() {
        assertInvalid("{}", "query");
        assertInvalid("{\"query\":\"  \"}", "query");
        assertInvalid("{\"query\":123}", "query");
        assertInvalid("{\"query\":\"" + "a".repeat(301) + "\"}", "300");
        assertInvalid("{\"query\":\"x\",\"max_price_cents\":0}", "max_price_cents");
        assertInvalid("{\"query\":\"x\",\"max_price_cents\":\"40\"}", "max_price_cents");
        assertInvalid("{\"query\":\"x\",\"max_price_cents\":12.5}", "max_price_cents");
        assertInvalid("{\"query\":\"x\",\"max_prep_minutes\":-5}", "max_prep_minutes");
        assertInvalid("{\"query\":\"x\",\"limit\":11}", "limit");
        assertInvalid("{\"query\":\"x\",\"dietary_tags\":\"vegano\"}", "dietary_tags");
        assertInvalid("{\"query\":\"x\",\"dietary_tags\":[1]}", "dietary_tags");
        assertInvalid("{\"query\":\"x\",\"dietary_tags\":[\"halal\"]}", "Restrição");
        assertInvalid("{\"query\":\"x\",\"cor\":\"azul\"}", "Campo desconhecido");
        assertInvalid("[1,2]", "objeto JSON");
        assertInvalid("não é json", "objeto JSON");

        verifyNoInteractions(service);
    }
}