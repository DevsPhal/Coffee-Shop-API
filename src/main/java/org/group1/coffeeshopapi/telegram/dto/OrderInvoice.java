package org.group1.coffeeshopapi.telegram.dto;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderInvoice(
        UUID orderId,
        List<OrderInvoiceLineItem> items,

        // Null for pickup; set for delivery.
        BigDecimal deliveryFee,

        BigDecimal totalAmount,
        PaymentMethod paymentMethod,

        // Set only when paid by Bakong in a non-USD currency.
        Currency bakongCurrency,
        BigDecimal bakongAmount,

        // Set only when paid in cash: how much was handed over and the change given back.
        BigDecimal amountTendered,
        Currency amountTenderedCurrency,
        BigDecimal changeDue,
        Currency changeCurrency,

        LocalDateTime paidAt
) {
}
