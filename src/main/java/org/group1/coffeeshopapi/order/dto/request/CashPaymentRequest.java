package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;

// A cash customer in Cambodia can pay in either USD or KHR, so staff picks which one the
// tendered amount is in rather than it being assumed USD.
public record CashPaymentRequest(
        @NotNull(message = "Currency is required")
        Currency currency,

        @NotNull(message = "Amount tendered is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount tendered must be greater than zero")
        BigDecimal amountTendered,

        // Which currency to give the change back in. Optional — defaults to the same currency
        // tendered, so USD in gives USD change and KHR in gives KHR change. Only set this to
        // switch it, e.g. a customer paying in KHR who wants USD change back.
        Currency changeCurrency
) {
}
