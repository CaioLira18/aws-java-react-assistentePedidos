package io.github.caiolira18.foodassistant.assistant.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Java puro: custo em USD a partir de tokens e preço por 1 milhão de tokens. */
public class CostCalculator {

    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    private final BigDecimal inputPerMillionUsd;
    private final BigDecimal outputPerMillionUsd;

    public CostCalculator(BigDecimal inputPerMillionUsd, BigDecimal outputPerMillionUsd) {
        if (inputPerMillionUsd == null || outputPerMillionUsd == null
                || inputPerMillionUsd.signum() < 0 || outputPerMillionUsd.signum() < 0) {
            throw new IllegalArgumentException("Preços por milhão de tokens devem ser informados e não negativos");
        }
        this.inputPerMillionUsd = inputPerMillionUsd;
        this.outputPerMillionUsd = outputPerMillionUsd;
    }

    /** Arredonda para 6 casas (HALF_UP), a escala da coluna cost_usd. */
    public BigDecimal cost(int inputTokens, int outputTokens) {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("Tokens não podem ser negativos");
        }
        BigDecimal total = inputPerMillionUsd.multiply(BigDecimal.valueOf(inputTokens))
                .add(outputPerMillionUsd.multiply(BigDecimal.valueOf(outputTokens)));
        return total.divide(MILLION, 6, RoundingMode.HALF_UP);
    }
}