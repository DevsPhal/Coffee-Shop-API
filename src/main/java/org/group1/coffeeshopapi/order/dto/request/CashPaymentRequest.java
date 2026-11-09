package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record CashPaymentRequest(
        @NotNull(message = "Currency is required")
        Currency currency,

        @NotNull(message = "Amount tendered is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount tendered must be greater than zero")
        @Digits(integer = ValidationPatterns.MONEY_INTEGER_DIGITS, fraction = 2, message = "Amount tendered must be a valid amount with at most 2 decimal places")
        BigDecimal amountTendered,

        Currency changeCurrency
) {
}
