package org.group1.coffeeshopapi.common.util;

import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KhrConversionTest {

    @ParameterizedTest(name = "{0} KHR -> {1} KHR")
    @CsvSource({
            // The examples from the rounding rule.
            "1250, 1200",
            "1251, 1300",
            "1249, 1200",
            "1299, 1300",
            "1350, 1300",
            "1351, 1400",
            // The edges of each band.
            "1200, 1200",
            "1201, 1200",
            "1300, 1300",
            "0, 0",
            "50, 0",
            "51, 100",
            "99, 100",
            "100, 100",
            "41000, 41000",
            "15621, 15600",
            "999950, 999900",
            "999951, 1000000",
            // Fractions of a riel round to a whole riel first, then the rule applies.
            "1250.4, 1200",
            "1250.5, 1300",
            "1349.99, 1300",
    })
    void roundsToTheNearest100WithFiftyRoundingDown(String raw, String expected) {
        assertThat(KhrConversion.roundToNearest100(new BigDecimal(raw))).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "${0} at {1} -> {2} KHR")
    @CsvSource({
            "0.30, 4100, 1200",     // 1,230 rounds down
            "1.19, 4100, 4900",     // 4,879 rounds up
            "10.00, 4100, 41000",   // exact
            "0.05, 4100, 200",      // 205 rounds down
            "3.81, 4100, 15600",    // 15,621 rounds down
            "1.25, 4050.5, 5100",   // 5,063.125 -> 5,063 -> 5,100
            "0.01, 4100, 0",        // 41 rounds down to nothing
    })
    void convertsUsdAndRoundsOnlyTheFinalRielAmount(String usd, String rate, String expected) {
        assertThat(KhrConversion.fromUsd(new BigDecimal(usd), new BigDecimal(rate))).isEqualByComparingTo(expected);
    }

    @Test
    void neverChangesTheUsdAmountOrTheRateItWasGiven() {
        BigDecimal usd = new BigDecimal("1.19");
        BigDecimal rate = new BigDecimal("4100.0000");

        KhrConversion.fromUsd(usd, rate);

        assertThat(usd).isEqualTo(new BigDecimal("1.19"));
        assertThat(rate).isEqualTo(new BigDecimal("4100.0000"));
    }

    @Test
    void negativeAmountsRoundTheSameWayAwayFromTheirSign() {
        assertThat(KhrConversion.roundToNearest100(new BigDecimal("-1250"))).isEqualByComparingTo("-1200");
        assertThat(KhrConversion.roundToNearest100(new BigDecimal("-1251"))).isEqualByComparingTo("-1300");
    }

    @Test
    void convertsRielBackToUsdCents() {
        assertThat(KhrConversion.toUsd(new BigDecimal("4000"), new BigDecimal("4100"))).isEqualByComparingTo("0.98");
        assertThat(KhrConversion.toUsd(new BigDecimal("41000"), new BigDecimal("4100"))).isEqualByComparingTo("10.00");
    }

    @Test
    void refusesAMissingOrZeroRate() {
        assertThatThrownBy(() -> KhrConversion.fromUsd(BigDecimal.ONE, null))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("exchange rate");
        assertThatThrownBy(() -> KhrConversion.fromUsd(BigDecimal.ONE, BigDecimal.ZERO))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("exchange rate");
        assertThatThrownBy(() -> KhrConversion.toUsd(BigDecimal.ONE, new BigDecimal("-1")))
                .isInstanceOf(InvalidOperationException.class).hasMessageContaining("exchange rate");
    }
}
