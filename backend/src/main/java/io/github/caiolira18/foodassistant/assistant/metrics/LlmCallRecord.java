package io.github.caiolira18.foodassistant.assistant.metrics;

import java.math.BigDecimal;

/** Uma chamada ao modelo, no formato da tabela llm_call. Tokens e custo ficam nulos se o provedor não informar uso. */
public record LlmCallRecord(String sessionId, String model, Integer inputTokens, Integer outputTokens,
        int latencyMs, BigDecimal costUsd) {
}