package io.github.caiolira18.foodassistant.catalog.search;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class DishSearchFiltersTest {

    @Test
    void rejectsUnknownTag() {
        assertThrows(IllegalArgumentException.class, () -> new DishSearchFilters(null, null, Set.of("halal")));
    }

    @Test
    void rejectsNonPositiveLimits() {
        assertThrows(IllegalArgumentException.class, () -> new DishSearchFilters(0, null, null));
        assertThrows(IllegalArgumentException.class, () -> new DishSearchFilters(null, -5, null));
    }

    @Test
    void noneHasNoRestrictions() {
        DishSearchFilters none = DishSearchFilters.none();
        assertTrue(none.requiredTags().isEmpty());
    }
}
