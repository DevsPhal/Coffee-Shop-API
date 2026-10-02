package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.StaffCallReason;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffCallResponse(
        UUID orderId,
        String customerName,
        OrderStatus orderStatus,
        FulfillmentMethod fulfillmentMethod,
        StaffCallReason reason,
        String note,
        LocalDateTime calledAt,
        LocalDateTime nextCallAllowedAt
) {
}
