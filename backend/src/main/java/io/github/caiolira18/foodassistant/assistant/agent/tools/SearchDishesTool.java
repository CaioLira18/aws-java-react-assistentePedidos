package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

import io.github.caiolira18.foodassistant.assistant.agent.AgentTool;
import io.github.caiolira18.foodassistant.assistant.agent.DishSummary;
import io.github.caiolira18.foodassistant.assistant.agent.ToolContext;
import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchFilters;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchResult;
import io.github.caiolira18.foodassistant.catalog.search.DishSearchService;

@Component
class SearchDishesTool implements AgentTool {

  static final String NAME = "search_dishes";
  static final int MAX_QUERY_LENGTH = 300;
  static final int DEFAULT_LIMIT = 5;
  static final int MAX_LIMIT = 10;

  private static final List<String> FIELDS = List.of(
      "query", "max_price_cents", "max_prep_minutes", "dietary_tags", "limit");

  private static final String DESCRIPTION = """
      Busca pratos no catálogo por significado, com filtros opcionais. \
      O preço é sempre em centavos de real (R$ 40,00 = 4000). \
      Retorna os pratos mais relevantes, do mais para o menos parecido com o pedido.""";

  private static final String SCHEMA = """
      {
        "type": "object",
        "properties": {
          "query": {"type": "string", "description": "O que o cliente quer comer, em linguagem natural."},
          "max_price_cents": {"type": "integer", "minimum": 1, "description": "Preço máximo em centavos de real."},
          "max_prep_minutes": {"type": "integer", "minimum": 1, "description": "Tempo máximo de preparo, em minutos."},
          "dietary_tags": {
            "type": "array",
            "items": {"type": "string", "enum": ["vegano", "vegetariano", "sem-gluten", "sem-lactose"]},
            "description": "Restrições obrigatórias. O prato precisa ter todas."
          },
          "limit": {"type": "integer", "minimum": 1, "maximum": 10, "description": "Quantidade de pratos. Padrão 5."}
        },
        "required": ["query"]
      }""";

  private static final ToolDefinition DEFINITION = new ToolDefinition(NAME, DESCRIPTION, SCHEMA);

  private final DishSearchService search;

  SearchDishesTool(DishSearchService search) {
    this.search = search;
  }

  @Override
  public ToolDefinition definition() {
    return DEFINITION;
  }

  @Override
  public String execute(String argumentsJson, ToolContext context) {
    ToolArguments args = ToolArguments.parse(argumentsJson, FIELDS);

    String query = args.requiredString("query", MAX_QUERY_LENGTH);
    Integer maxPriceCents = args.optionalInt("max_price_cents", 1, 1_000_000);
    Integer maxPrepMinutes = args.optionalInt("max_prep_minutes", 1, 1_440);
    Integer limit = args.optionalInt("limit", 1, MAX_LIMIT);

    List<String> tags = args.optionalStringList("dietary_tags", 4).stream()
        .map(SearchDishesTool::normalizeTag)
        .distinct()
        .toList();
    for (String tag : tags) {
      if (!DishSearchFilters.ALLOWED_TAGS.contains(tag)) {
        throw new ToolException("Restrição desconhecida. Válidas: "
            + String.join(", ", new TreeSet<>(DishSearchFilters.ALLOWED_TAGS)));
      }
    }

    DishSearchFilters filters = new DishSearchFilters(maxPriceCents, maxPrepMinutes, Set.copyOf(tags));
    List<DishSearchResult> results = search.search(query, filters, limit == null ? DEFAULT_LIMIT : limit);
    if (!results.isEmpty()) {
      context.events().dishesFound(results.stream()
          .map(r -> new DishSummary(r.id(), r.name(), r.description(), r.priceCents(),
              r.prepMinutes(), r.dietaryTags()))
          .toList());
    }

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("count", results.size());
    response.put("dishes", results.stream().map(SearchDishesTool::toMap).toList());
    if (results.isEmpty()) {
      response.put("note", "Nenhum prato encontrado com esses filtros. Tente relaxar preço, tempo ou restrições.");
    }
    return ToolJson.write(response);
  }

  private static Map<String, Object> toMap(DishSearchResult r) {
    Map<String, Object> map = DishToolFormat.dishMap(
        r.id(), r.name(), r.description(), r.priceCents(), r.prepMinutes(), r.dietaryTags());
    map.put("similarity", DishToolFormat.round3(r.similarity()));
    return map;
  }

  static String normalizeTag(String raw) {
    String lower = raw.strip().toLowerCase(Locale.ROOT);
    return Normalizer.normalize(lower, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace(' ', '-');
  }
}