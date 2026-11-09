package io.github.caiolira18.foodassistant.assistant.metrics;

import java.time.Duration;

import io.github.caiolira18.foodassistant.assistant.llm.Usage;

/** Registra uma chamada ao modelo. Nunca deve lançar exceção para quem chama. */
public interface LlmCallRecorder {

    /** Não registra nada. Usado pelos construtores antigos do orquestrador e em testes. */
    LlmCallRecorder NOOP = (sessionId, usage, latency) -> {
    };

    void record(String sessionId, Usage usage, Duration latency);
}