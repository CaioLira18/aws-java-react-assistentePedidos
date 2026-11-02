package io.github.caiolira18.foodassistant.catalog.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.catalog.embedding.FakeEmbeddingClient;

class DishSearchServiceTest {

    private final DishSearchRepository repository = mock(DishSearchRepository.class);
    private final DishSearchService service = new DishSearchService(new FakeEmbeddingClient(1024), repository);

    @Test
    void rejectsBlankQueryWithoutTouchingTheDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.search("  ", null, 5));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () -> service.search("vegano", null, 0));
        assertThrows(IllegalArgumentException.class, () -> service.search("vegano", null, DishSearchService.MAX_LIMIT + 1));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsTooLongQuery() {
        String tooLong = "a".repeat(DishSearchService.MAX_QUERY_LENGTH + 1);
        assertThrows(IllegalArgumentException.class, () -> service.search(tooLong, null, 5));
    }

    @Test
    void usesNoFiltersWhenNullAndDelegatesToRepository() {
        List<DishSearchResult> expected = List.of(
                new DishSearchResult(1, "Feijoada Vegana", "desc", 3190, 45, List.of("vegano"), 0.9));
        when(repository.search(any(float[].class), eq(DishSearchFilters.none()), eq(5))).thenReturn(expected);

        assertEquals(expected, service.search("feijoada vegana", null, 5));
        verify(repository).search(any(float[].class), eq(DishSearchFilters.none()), eq(5));
    }
}
