package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.OrderStatus;

import java.math.BigDecimal;

public record OrderStatusSummaryResponse(
        OrderStatus status,
        long count,
        BigDecimal totalAmount
) {
}
