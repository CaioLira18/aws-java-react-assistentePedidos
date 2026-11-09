package io.github.caiolira18.foodassistant.assistant.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CostCalculatorTest {

    private final CostCalculator calc = new CostCalculator(new BigDecimal("3.00"), new BigDecimal("15.00"));

    @Test
    void computesKnownValue() {
        // 1000 * 3/1M + 500 * 15/1M = 0.003 + 0.0075
        assertThat(calc.cost(1000, 500)).isEqualByComparingTo("0.010500");
    }

    @Test
    void zeroTokensCostNothing() {
        assertThat(calc.cost(0, 0)).isEqualByComparingTo("0");
    }

    @Test
    void roundsHalfUpToSixDecimals() {
        CostCalculator half = new CostCalculator(new BigDecimal("0.5"), BigDecimal.ZERO);
        assertThat(half.cost(1, 0)).isEqualByComparingTo("0.000001");
    }

    @Test
    void rejectsNegativeTokens() {
        assertThatThrownBy(() -> calc.cost(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calc.cost(0, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPrices() {
        assertThatThrownBy(() -> new CostCalculator(null, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CostCalculator(new BigDecimal("-1"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}