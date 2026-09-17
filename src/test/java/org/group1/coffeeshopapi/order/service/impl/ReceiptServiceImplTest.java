package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Covers which order states may have a receipt printed. Before this, only OrderStatus.COMPLETED
 * (pickup) was accepted, silently making it impossible to ever print a receipt for a delivered
 * order — see ReceiptServiceImpl#generateReceiptPdf.
 */
@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @Mock private OrderService orderService;
    @Mock private BakongProperties bakongProperties;
    @InjectMocks private ReceiptServiceImpl service;

    @Test
    void aCompletedPickupOrderGetsAReceipt() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.COMPLETED));

        byte[] pdf = service.generateReceiptPdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void aDeliveredDeliveryOrderAlsoGetsAReceipt() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.DELIVERED));

        byte[] pdf = service.generateReceiptPdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void anOrderStillInProgressHasNoReceiptYet() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.OUT_FOR_DELIVERY));

        assertThatThrownBy(() -> service.generateReceiptPdf(orderId))
                .isInstanceOf(InvalidOperationException.class);
    }

    private OrderResponse order(OrderStatus status) {
        return new OrderResponse(
                UUID.randomUUID(), null, null, null, null, null,
                status, List.of(), new BigDecimal("10.00"),
                FulfillmentMethod.PICKUP, null, null, null, null, null,
                PaymentMethod.CASH, new BigDecimal("10.00"), Currency.USD, BigDecimal.ZERO,
                null, null, null, null,
                null, LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null);
    }
}
