package org.group1.coffeeshopapi.realtime.event;

import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;

// Raised inside the order transaction; only pushed to clients once it commits.
// customerEmail is null for a walk-in sale.
public record OrderChangedEvent(OrderAuditAction action, OrderResponse order, String customerEmail) {
}
