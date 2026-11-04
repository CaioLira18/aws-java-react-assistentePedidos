package io.github.caiolira18.foodassistant.assistant.llm;

public record ToolCall(String id, String name, String argumentsJson) {}
