package io.github.caiolira18.foodassistant.assistant.agent;

public record ToolContext(String sessionId, ToolEvents events) {
    public ToolContext {
        events = events == null ? ToolEvents.NONE : events;
    }

    public ToolContext(String sessionId) {
        this(sessionId, ToolEvents.NONE);
    }
}