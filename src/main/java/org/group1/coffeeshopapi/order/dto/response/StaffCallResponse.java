package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

// One open (unanswered) call, or the result of pressing "call staff". nextCallAllowedAt is when
// the customer's button can be enabled again.
public record StaffCallResponse(
        UUID orderId,
        String customerName,
        OrderStatus orderStatus,
        FulfillmentMethod fulfillmentMethod,
        LocalDateTime calledAt,
        LocalDateTime nextCallAllowedAt
) {
}
