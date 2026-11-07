package io.github.caiolira18.foodassistant.ordering;

public record CartLine(long dishId, String name, int unitPriceCents, int quantity, int lineTotalCents) {
}