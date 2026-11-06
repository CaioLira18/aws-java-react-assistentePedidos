package io.github.caiolira18.foodassistant.assistant.agent;

import java.time.Duration;

public record AgentSettings(int maxIterations, Duration timeout, int maxTokens,
        int maxHistoryMessages, int maxToolCallsPerTurn) {

    public AgentSettings {
        if (maxIterations < 1 || maxTokens < 1 || maxHistoryMessages < 1 || maxToolCallsPerTurn < 1) {
            throw new IllegalArgumentException("Limites do agente devem ser positivos");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout deve ser positivo");
        }
    }

    public static AgentSettings defaults() {
        return new AgentSettings(5, Duration.ofSeconds(30), 1024, 20, 4);
    }
}