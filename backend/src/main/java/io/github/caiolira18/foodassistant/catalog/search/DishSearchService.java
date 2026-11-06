package io.github.caiolira18.foodassistant.catalog.search;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.github.caiolira18.foodassistant.catalog.embedding.EmbeddingClient;

@Service
public class DishSearchService {

    static final int MAX_LIMIT = 20;
    static final int MAX_QUERY_LENGTH = 300;

    private final EmbeddingClient embeddings;
    private final DishSearchRepository repository;

    DishSearchService(EmbeddingClient embeddings, DishSearchRepository repository) {
        this.embeddings = embeddings;
        this.repository = repository;
    }

    public List<DishSearchResult> search(String query, DishSearchFilters filters, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("A consulta não pode ser vazia");
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("Consulta maior que " + MAX_QUERY_LENGTH + " caracteres");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit deve estar entre 1 e " + MAX_LIMIT);
        }
        DishSearchFilters effective = filters == null ? DishSearchFilters.none() : filters;
        return repository.search(embeddings.embed(query.strip()), effective, limit);
    }

    public Optional<DishDetails> getDish(long id) {
        if (id < 1) {
            return Optional.empty();
        }
        return repository.findActiveById(id);
    }
}
