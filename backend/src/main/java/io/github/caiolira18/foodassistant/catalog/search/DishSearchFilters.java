package io.github.caiolira18.foodassistant.catalog.search;

import java.util.Set;

public record DishSearchFilters(Integer maxPriceCents, Integer maxPrepMinutes, Set<String> requiredTags) {

    public static final Set<String> ALLOWED_TAGS = Set.of("vegano", "vegetariano", "sem-gluten", "sem-lactose");

    public DishSearchFilters {
        if (maxPriceCents != null && maxPriceCents <= 0) {
            throw new IllegalArgumentException("maxPriceCents deve ser positivo");
        }
        if (maxPrepMinutes != null && maxPrepMinutes <= 0) {
            throw new IllegalArgumentException("maxPrepMinutes deve ser positivo");
        }
        requiredTags = requiredTags == null ? Set.of() : Set.copyOf(requiredTags);
        for (String tag : requiredTags) {
            if (!ALLOWED_TAGS.contains(tag)) {
                throw new IllegalArgumentException("Restrição desconhecida: " + tag);
            }
        }
    }

    public static DishSearchFilters none() {
        return new DishSearchFilters(null, null, Set.of());
    }
}
