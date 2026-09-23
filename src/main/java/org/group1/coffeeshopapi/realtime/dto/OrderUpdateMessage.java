package org.group1.coffeeshopapi.realtime.dto;

import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;

import java.time.LocalDateTime;

// What clients receive. order is the full, current order — replace your local copy by order.id.
public record OrderUpdateMessage(OrderAuditAction action, OrderResponse order, LocalDateTime sentAt) {
}
