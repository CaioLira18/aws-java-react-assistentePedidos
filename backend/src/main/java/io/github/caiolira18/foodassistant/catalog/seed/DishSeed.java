package io.github.caiolira18.foodassistant.catalog.seed;

import java.util.List;

public record DishSeed(
    String name,
    String description,
    int priceCents,
    int prepMinutes,
    List<String> dietaryTags) {
}
