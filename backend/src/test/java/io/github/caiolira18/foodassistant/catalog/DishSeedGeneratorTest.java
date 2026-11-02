package io.github.caiolira18.foodassistant.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.caiolira18.foodassistant.catalog.seed.DishSeed;
import io.github.caiolira18.foodassistant.catalog.seed.DishSeedGenerator;

class DishSeedGeneratorTest {

    private final List<DishSeed> dishes = DishSeedGenerator.generate();

    @Test
    void generatesExpectedAmount() {
        assertEquals(150, dishes.size());
    }

    @Test
    void namesAreUnique() {
        assertEquals(dishes.size(), dishes.stream().map(DishSeed::name).distinct().count());
    }

    @Test
    void pricesAndPrepTimesArePositive() {
        assertTrue(dishes.stream().allMatch(d -> d.priceCents() > 0 && d.prepMinutes() > 0));
    }

    @Test
    void veganImpliesVegetarianAndLactoseFree() {
        dishes.stream()
                .filter(d -> d.dietaryTags().contains("vegano"))
                .forEach(d -> {
                    assertTrue(d.dietaryTags().contains("vegetariano"));
                    assertTrue(d.dietaryTags().contains("sem-lactose"));
                });
    }

    @Test
    void isDeterministic() {
        assertEquals(dishes, DishSeedGenerator.generate());
    }
}
