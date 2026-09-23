package org.group1.coffeeshopapi.realtime.dto;

import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.UUID;

// type is CALLED (show the alert) or ANSWERED (dismiss it). answeredByName is null for CALLED.
public record StaffCallMessage(
        Type type,
        UUID orderId,
        String customerName,
        OrderStatus orderStatus,
        FulfillmentMethod fulfillmentMethod,
        LocalDateTime calledAt,
        String answeredByName,
        LocalDateTime sentAt
) {
    public enum Type { CALLED, ANSWERED }
}
