package io.github.caiolira18.foodassistant.assistant.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.caiolira18.foodassistant.assistant.llm.ToolCall;
import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;

public class ToolRegistry {

    static final int MAX_ARGUMENTS_LENGTH = 2000;

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AgentTool> registered) {
        for (AgentTool tool : registered) {
            String name = tool.definition().name();
            if (tools.putIfAbsent(name, tool) != null) {
                throw new IllegalArgumentException("Tool duplicada: " + name);
            }
        }
    }

    public List<ToolDefinition> definitions() {
        return tools.values().stream().map(AgentTool::definition).toList();
    }

    public String execute(ToolCall call, ToolContext context) {
        AgentTool tool = tools.get(call.name());
        if (tool == null) {
            return errorJson("Ferramenta desconhecida. Disponíveis: " + String.join(", ", tools.keySet()));
        }
        String arguments = call.argumentsJson() == null ? "{}" : call.argumentsJson();
        if (arguments.length() > MAX_ARGUMENTS_LENGTH) {
            return errorJson("Argumentos grandes demais");
        }
        try {
            return tool.execute(arguments, context);
        } catch (ToolException e) {
            return errorJson(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Falha ao executar a tool {}", call.name(), e);
            return errorJson("Falha interna ao executar a ferramenta");
        }
    }

    static String errorJson(String message) {
        StringBuilder sb = new StringBuilder("{\"error\":\"");
        for (char c : message.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append("\"}").toString();
    }
}