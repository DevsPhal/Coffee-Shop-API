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

        BigDecimal deliveryFee,

        BigDecimal totalAmount,
        PaymentMethod paymentMethod,

        Currency bakongCurrency,
        BigDecimal bakongAmount,

        BigDecimal amountTendered,
        Currency amountTenderedCurrency,
        BigDecimal changeDue,
        Currency changeCurrency,

        LocalDateTime paidAt
) {
}
