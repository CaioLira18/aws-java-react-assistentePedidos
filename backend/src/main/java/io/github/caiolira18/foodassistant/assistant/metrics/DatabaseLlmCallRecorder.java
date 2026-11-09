package io.github.caiolira18.foodassistant.assistant.metrics;

import java.math.BigDecimal;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.caiolira18.foodassistant.assistant.llm.Usage;

public class DatabaseLlmCallRecorder implements LlmCallRecorder {

    private static final Logger log = LoggerFactory.getLogger(DatabaseLlmCallRecorder.class);

    private final LlmCallRepository repository;
    private final CostCalculator calculator;
    private final String model;

    public DatabaseLlmCallRecorder(LlmCallRepository repository, CostCalculator calculator, String model) {
        this.repository = repository;
        this.calculator = calculator;
        this.model = model;
    }

    @Override
    public void record(String sessionId, Usage usage, Duration latency) {
        try {
            Integer in = usage == null ? null : usage.inputTokens();
            Integer out = usage == null ? null : usage.outputTokens();
            BigDecimal cost = usage == null ? null : calculator.cost(in, out);
            long ms = Math.max(0, latency.toMillis());
            int latencyMs = (int) Math.min(ms, Integer.MAX_VALUE);
            repository.insert(new LlmCallRecord(sessionId, model, in, out, latencyMs, cost));
        } catch (RuntimeException e) {
            // métrica nunca pode derrubar a resposta ao usuário
            log.warn("Falha ao registrar métrica da chamada ao modelo", e);
        }
    }
}