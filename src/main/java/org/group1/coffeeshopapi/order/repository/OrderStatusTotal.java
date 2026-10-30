package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.common.enums.OrderStatus;

import java.math.BigDecimal;

public record OrderStatusTotal(OrderStatus status, long count, BigDecimal totalAmount) {
}
