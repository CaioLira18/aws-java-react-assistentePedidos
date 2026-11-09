package io.github.caiolira18.foodassistant.assistant.metrics;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MetricsConfig {

    @Bean
    CostCalculator costCalculator(
            @Value("${app.llm.pricing.input-per-million-usd:0}") BigDecimal inputPerMillionUsd,
            @Value("${app.llm.pricing.output-per-million-usd:0}") BigDecimal outputPerMillionUsd) {
        return new CostCalculator(inputPerMillionUsd, outputPerMillionUsd);
    }

    @Bean
    LlmCallRecorder llmCallRecorder(LlmCallRepository repository, CostCalculator calculator,
            @Value("${app.llm.model-id:unknown}") String modelId) {
        return new DatabaseLlmCallRecorder(repository, calculator, modelId);
    }
}