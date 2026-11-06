package io.github.caiolira18.foodassistant.assistant.agent;

public enum AgentOutcome {
    COMPLETED,
    MAX_TOKENS,
    ITERATION_LIMIT,
    TIMEOUT
}