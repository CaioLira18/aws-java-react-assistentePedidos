package io.github.caiolira18.foodassistant.assistant.agent;

import io.github.caiolira18.foodassistant.assistant.llm.ToolDefinition;

public interface AgentTool {

    ToolDefinition definition();

    /** Valida os argumentos e executa. Lança {@link ToolException} para entrada inválida. */
    String execute(String argumentsJson, ToolContext context);
}