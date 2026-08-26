package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        // Who rang up/fulfilled/served this sale — an admin or a barista. Set for a POS sale from
        // the start, and also set on a customer order once staff collects cash or confirms
        // payment for it. See order/{id}/history for the full handling audit trail.
        UUID handledById,
        String handledByName,
        Role handledByRole,
        // Who placed this order — null for a barista POS sale rung up for a walk-in customer.
        UUID customerId,
        String customerName,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        BigDecimal amountTendered,
        BigDecimal changeDue,
        String bakongQrString,
        String bakongMd5Hash,
        Currency bakongCurrency,
        BigDecimal bakongAmount,
        String note,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
