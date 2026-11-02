package io.github.caiolira18.foodassistant.catalog.search;

import java.util.List;

public record DishSearchResult(
        long id,
        String name,
        String description,
        int priceCents,
        int prepMinutes,
        List<String> dietaryTags,
        double similarity) {
}
