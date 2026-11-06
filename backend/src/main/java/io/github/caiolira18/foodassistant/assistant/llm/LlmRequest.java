package io.github.caiolira18.foodassistant.assistant.llm;

import java.util.List;

public record LlmRequest(String systemPrompt, List<Message> messages,
        List<ToolDefinition> tools, int maxTokens) {
    public LlmRequest {
        messages = List.copyOf(messages);
        tools = List.copyOf(tools);

        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("A requisição precisa de mensagens");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens deve ser positivo");
        }
        systemPrompt = systemPrompt == null ? "" : systemPrompt;
        messages = List.copyOf(messages);
        tools = tools == null ? List.of() : List.copyOf(tools);
    }
}
