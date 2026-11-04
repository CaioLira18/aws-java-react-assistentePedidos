package io.github.caiolira18.foodassistant.assistant.llm;

import java.util.List;

public record LlmRequest(String systemPrompt, List<Message> messages,
    List<ToolDefinition> tools, int maxTokens) {
    public LlmRequest {
        messages = List.copyOf(messages);
        tools = List.copyOf(tools);
    }
}
