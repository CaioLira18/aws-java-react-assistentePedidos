package io.github.caiolira18.foodassistant.assistant.llm;

import java.util.List;

public record Message(Role role, String text, List<ToolCall> toolCalls, String toolCallId) {
    public enum Role { USER, ASSISTANT, TOOL }
    public Message { toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls); }
    public static Message user(String text) { return new Message(Role.USER, text, null, null); }
    public static Message assistant(String text, List<ToolCall> calls) { return new Message(Role.ASSISTANT, text, calls, null); }
    public static Message toolResult(String toolCallId, String json) { return new Message(Role.TOOL, json, null, toolCallId); }
}
