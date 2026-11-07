package io.github.caiolira18.foodassistant.ordering;

import java.util.List;

public record CartView(List<CartLine> items, int totalItems, int totalCents) {

    public CartView {
        items = List.copyOf(items);
    }

    public static CartView empty() {
        return new CartView(List.of(), 0, 0);
    }
}