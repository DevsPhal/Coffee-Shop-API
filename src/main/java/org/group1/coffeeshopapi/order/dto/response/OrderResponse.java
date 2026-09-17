package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        // Which staff member rang up or fulfilled this sale. Set from the start for a POS sale,
        // or once staff collects cash/confirms payment for a customer order.
        UUID handledById,
        String handledByName,
        Role handledByRole,
        // Who placed this order — null for a walk-in sale rung up by staff.
        UUID customerId,
        String customerName,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        // For DELIVERY, the address/contact a courier needs. dispatchedAt/deliveredAt stay null
        // until the order actually gets there.
        FulfillmentMethod fulfillmentMethod,
        String deliveryAddress,
        String contactName,
        String contactPhone,
        LocalDateTime dispatchedAt,
        LocalDateTime deliveredAt,
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
        // Null for a pickup order. Both set together or not at all.
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        // Null until staff evaluates it, even for a delivery order. Already folded into totalAmount.
        BigDecimal deliveryFee,
        // Straight-line distance from the shop to the delivery location — null for pickup, or if
        // the shop's own location isn't configured.
        BigDecimal distanceMeters
) {
}
