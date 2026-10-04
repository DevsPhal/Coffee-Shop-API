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
        UUID handledById,
        String handledByName,
        Role handledByRole,
        UUID customerId,
        String customerName,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalAmount,
        FulfillmentMethod fulfillmentMethod,
        UUID tableId,
        String tableNumber,
        String deliveryAddress,
        String contactName,
        String contactPhone,
        LocalDateTime dispatchedAt,
        LocalDateTime deliveredAt,
        PaymentMethod paymentMethod,
        BigDecimal amountTendered,
        Currency amountTenderedCurrency,
        BigDecimal changeDue,
        Currency changeCurrency,
        String bakongQrString,
        String bakongMd5Hash,
        Currency bakongCurrency,
        BigDecimal bakongAmount,
        String note,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        BigDecimal deliveryFee,
        BigDecimal distanceMeters,
        LocalDateTime deliveryFeeSetAt,
        boolean awaitingDeliveryFee,
        BigDecimal itemsTotal,
        LocalDateTime estimatedReadyAt,
        LocalDateTime bakongExpiresAt
) {
}
