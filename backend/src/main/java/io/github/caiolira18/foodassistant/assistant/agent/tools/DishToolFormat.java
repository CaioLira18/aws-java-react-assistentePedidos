package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DishToolFormat {

    static final int MAX_DESCRIPTION_LENGTH = 300;

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private DishToolFormat() {
    }

    static Map<String, Object> dishMap(long id, String name, String description, int priceCents,
            int prepMinutes, List<String> dietaryTags) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("description", truncate(description, MAX_DESCRIPTION_LENGTH));
        map.put("price_cents", priceCents);
        map.put("price_brl", priceBrl(priceCents));
        map.put("prep_minutes", prepMinutes);
        map.put("dietary_tags", List.copyOf(dietaryTags));
        return map;
    }

    static String priceBrl(int cents) {
        return String.format(PT_BR, "R$ %,.2f", BigDecimal.valueOf(cents, 2));
    }

    static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1).stripTrailing() + "…";
    }

    static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}