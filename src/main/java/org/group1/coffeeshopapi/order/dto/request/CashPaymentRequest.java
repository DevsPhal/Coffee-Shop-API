package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;

// currency is a click-option (same USD/KHR enum Bakong QR generation already uses), not free
// text — a customer paying cash in Cambodia can hand over either, so staff picks which one this
// tendered amount is in rather than it being assumed USD. See OrderServiceImpl.chargeCash for how
// a KHR amount is converted to its USD equivalent to compare against/subtract from the total.
public record CashPaymentRequest(
        @NotNull(message = "Currency is required")
        Currency currency,

        @NotNull(message = "Amount tendered is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount tendered must be greater than zero")
        BigDecimal amountTendered
) {
}
