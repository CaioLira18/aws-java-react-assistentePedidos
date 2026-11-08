package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.caiolira18.foodassistant.assistant.agent.ToolException;

final class ToolArguments {

    private static final int MAX_KEY_PREVIEW = 40;

    private final Map<String, Object> values;

    private ToolArguments(Map<String, Object> values) {
        this.values = values;
    }

    static ToolArguments parse(String json, List<String> allowedFields) {
        Map<String, Object> values = ToolJson.parseObject(json);
        for (String key : values.keySet()) {
            if (!allowedFields.contains(key)) {
                String shown = key.length() > MAX_KEY_PREVIEW ? key.substring(0, MAX_KEY_PREVIEW) + "…" : key;
                throw new ToolException(
                        "Campo desconhecido: " + shown + ". Campos aceitos: " + String.join(", ", allowedFields));
            }
        }
        return new ToolArguments(values);
    }

    String requiredString(String name, int maxLength) {
        Object value = values.get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ToolException(name + " é obrigatório e deve ser um texto");
        }
        String trimmed = text.strip();
        if (trimmed.length() > maxLength) {
            throw new ToolException(name + " deve ter no máximo " + maxLength + " caracteres");
        }
        return trimmed;
    }

    int requiredInt(String name, int min, int max) {
        Integer value = optionalInt(name, min, max);
        if (value == null) {
            throw new ToolException(name + " é obrigatório");
        }
        return value;
    }

    Integer optionalInt(String name, int min, int max) {
        Object value = values.get(name);
        if (value == null) {
            return null;
        }
        int number = toInt(name, value);
        if (number < min || number > max) {
            throw new ToolException(name + " deve estar entre " + min + " e " + max);
        }
        return number;
    }

    List<String> optionalStringList(String name, int maxItems) {
        Object value = values.get(name);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ToolException(name + " deve ser uma lista de textos");
        }
        if (list.size() > maxItems) {
            throw new ToolException(name + " aceita no máximo " + maxItems + " itens");
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof String text)) {
                throw new ToolException(name + " deve conter apenas textos");
            }
            result.add(text);
        }
        return result;
    }

    private static int toInt(String name, Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Long l && l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
            return l.intValue();
        }
        throw new ToolException(name + " deve ser um número inteiro");
    }
}