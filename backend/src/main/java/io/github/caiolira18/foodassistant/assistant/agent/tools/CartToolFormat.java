package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.caiolira18.foodassistant.ordering.CartView;

final class CartToolFormat {

    private CartToolFormat() {
    }

    static String toJson(CartView cart) {
        List<Map<String, Object>> items = cart.items().stream().map(line -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("dish_id", line.dishId());
            map.put("name", line.name());
            map.put("quantity", line.quantity());
            map.put("unit_price_cents", line.unitPriceCents());
            map.put("unit_price_brl", DishToolFormat.priceBrl(line.unitPriceCents()));
            map.put("line_total_cents", line.lineTotalCents());
            map.put("line_total_brl", DishToolFormat.priceBrl(line.lineTotalCents()));
            return map;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("is_empty", items.isEmpty());
        response.put("items", items);
        response.put("total_items", cart.totalItems());
        response.put("total_cents", cart.totalCents());
        response.put("total_brl", DishToolFormat.priceBrl(cart.totalCents()));
        return ToolJson.write(response);
    }
}