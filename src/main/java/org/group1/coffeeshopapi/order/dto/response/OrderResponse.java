package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        // Who rang up/fulfilled/served this sale — an admin or a barista. Set for a POS sale from
        // the start, and also set on a customer order once staff collects cash or confirms
        // payment for it. See order/{id}/history for the full handling audit trail.
        UUID handledById,
        String handledByName,
        Role handledByRole,
        // Who placed this order — null for a barista POS sale rung up for a walk-in customer.
        UUID customerId,
        String customerName,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        BigDecimal amountTendered,
        Currency amountTenderedCurrency,
        BigDecimal changeDue,
        String bakongQrString,
        String bakongMd5Hash,
        Currency bakongCurrency,
        BigDecimal bakongAmount,
        String note,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        // Null for a pickup order (every POS sale, or a customer order that didn't pin a
        // location). Both set together or not at all — see Order.isDelivery.
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        // Null until an admin/barista evaluates it (see OrderService.setDeliveryFee), even for a
        // delivery order. Already folded into totalAmount once set.
        BigDecimal deliveryFee,
        // Straight-line distance from the shop to deliveryLatitude/deliveryLongitude, for whoever
        // is evaluating the fee above — null for a pickup order, or if shop.location isn't
        // configured (see ShopLocationProperties).
        BigDecimal distanceMeters
) {
}
