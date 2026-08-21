package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashPaymentRequest(
        @NotNull(message = "Amount tendered is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount tendered must be greater than zero")
        BigDecimal amountTendered
) {
}
