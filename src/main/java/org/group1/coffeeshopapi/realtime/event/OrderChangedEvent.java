package org.group1.coffeeshopapi.realtime.event;

import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;

public record OrderChangedEvent(OrderAuditAction action, OrderResponse order, String customerEmail) {
}
