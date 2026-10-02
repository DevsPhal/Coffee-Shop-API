package org.group1.coffeeshopapi.realtime.dto;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.StaffCallReason;

import java.time.LocalDateTime;
import java.util.UUID;

public record StaffCallMessage(
        Type type,
        UUID orderId,
        String customerName,
        OrderStatus orderStatus,
        FulfillmentMethod fulfillmentMethod,
        StaffCallReason reason,
        String note,
        LocalDateTime calledAt,
        String answeredByName,
        String reply,
        LocalDateTime sentAt
) {
    public enum Type { CALLED, ANSWERED }
}
