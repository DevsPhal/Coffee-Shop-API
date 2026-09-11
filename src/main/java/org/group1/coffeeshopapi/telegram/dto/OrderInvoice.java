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

        // Null for a walk-in/pickup order; set for a delivery order — shown as its own line
        // alongside the items subtotal so totalAmount (which already includes it) never looks
        // like it doesn't add up to what's itemized above it.
        BigDecimal deliveryFee,

        BigDecimal totalAmount,
        PaymentMethod paymentMethod,

        // Set only when paymentMethod is BAKONG and the QR wasn't generated in USD — the amount
        // actually charged in that currency (see Order.bakongAmount), shown alongside totalAmount
        // (always USD) so the invoice makes sense regardless of which currency was paid.
        Currency bakongCurrency,
        BigDecimal bakongAmount,

        LocalDateTime paidAt
) {
}
