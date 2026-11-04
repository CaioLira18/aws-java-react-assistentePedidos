package io.github.caiolira18.foodassistant.assistant.llm;

import java.util.List;

public record LlmResponse(String text, List<ToolCall> toolCalls, Usage usage, StopReason stopReason) {
    public enum StopReason { END_TURN, TOOL_USE, MAX_TOKENS }
    public LlmResponse { toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls); }
    public boolean wantsTools() { return !toolCalls.isEmpty(); }
}
