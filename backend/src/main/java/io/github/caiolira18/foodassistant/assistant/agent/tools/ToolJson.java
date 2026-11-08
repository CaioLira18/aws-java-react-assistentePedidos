package io.github.caiolira18.foodassistant.assistant.agent.tools;

import java.util.Map;

import io.github.caiolira18.foodassistant.assistant.agent.ToolException;
import tools.jackson.databind.json.JsonMapper;

final class ToolJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private ToolJson() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(String json) {
        try {
            Object parsed = MAPPER.readValue(json, Object.class);
            if (parsed instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        } catch (RuntimeException e) {
            // cai no erro padronizado abaixo
        }
        throw new ToolException("Os argumentos precisam ser um objeto JSON válido");
    }

    static String write(Object value) {
        return MAPPER.writeValueAsString(value);
    }
}