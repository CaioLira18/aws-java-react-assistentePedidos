package io.github.caiolira18.foodassistant.assistant.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.caiolira18.foodassistant.assistant.llm.Usage;

class DatabaseLlmCallRecorderTest {

    private final LlmCallRepository repository = mock(LlmCallRepository.class);
    private final CostCalculator calculator = new CostCalculator(new BigDecimal("3.00"), new BigDecimal("15.00"));
    private final DatabaseLlmCallRecorder recorder = new DatabaseLlmCallRecorder(repository, calculator, "test-model");

    @Test
    void recordsTokensLatencyAndCost() {
        recorder.record("s1", new Usage(1000, 500), Duration.ofMillis(850));

        ArgumentCaptor<LlmCallRecord> captor = ArgumentCaptor.forClass(LlmCallRecord.class);
        verify(repository).insert(captor.capture());
        LlmCallRecord r = captor.getValue();
        assertThat(r.sessionId()).isEqualTo("s1");
        assertThat(r.model()).isEqualTo("test-model");
        assertThat(r.inputTokens()).isEqualTo(1000);
        assertThat(r.outputTokens()).isEqualTo(500);
        assertThat(r.latencyMs()).isEqualTo(850);
        assertThat(r.costUsd()).isEqualByComparingTo("0.010500");
    }

    @Test
    void recordsNullsWhenUsageIsMissing() {
        recorder.record("s1", null, Duration.ofMillis(10));

        ArgumentCaptor<LlmCallRecord> captor = ArgumentCaptor.forClass(LlmCallRecord.class);
        verify(repository).insert(captor.capture());
        assertThat(captor.getValue().inputTokens()).isNull();
        assertThat(captor.getValue().outputTokens()).isNull();
        assertThat(captor.getValue().costUsd()).isNull();
    }

    @Test
    void repositoryFailureNeverPropagates() {
        doThrow(new IllegalStateException("db fora")).when(repository).insert(any());

        assertThatCode(() -> recorder.record("s1", new Usage(1, 1), Duration.ofMillis(1)))
                .doesNotThrowAnyException();
    }
}