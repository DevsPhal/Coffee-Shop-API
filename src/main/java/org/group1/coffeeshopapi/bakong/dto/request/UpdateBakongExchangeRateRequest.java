package org.group1.coffeeshopapi.bakong.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateBakongExchangeRateRequest(
        @NotNull(message = "Exchange rate is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Exchange rate must be greater than zero")
        BigDecimal khrPerUsdRate
) {
}
