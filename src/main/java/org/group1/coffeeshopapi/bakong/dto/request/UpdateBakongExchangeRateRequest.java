package org.group1.coffeeshopapi.bakong.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBakongExchangeRateRequest(
        @NotNull(message = "Exchange rate is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Exchange rate must be greater than zero")
        BigDecimal khrPerUsdRate,

        // Optional: the real-world market rate, kept only as a reference alongside khrPerUsdRate
        // (see BakongExchangeRate's javadoc) — omit to leave it unchanged.
        @DecimalMin(value = "0.0", inclusive = false, message = "Market rate must be greater than zero")
        BigDecimal marketRate
) {
}
