package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID baristaId,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        BigDecimal amountTendered,
        BigDecimal changeDue,
        String bakongQrString,
        String bakongMd5Hash,
        String note,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
}
