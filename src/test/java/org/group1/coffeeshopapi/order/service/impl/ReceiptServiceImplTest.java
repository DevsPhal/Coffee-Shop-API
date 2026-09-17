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

// Covers which order states may get a printed document: a receipt needs the order fully
// finished, while an invoice only needs it paid.
@ExtendWith(MockitoExtension.class)
class ReceiptServiceImplTest {

    @Mock private OrderService orderService;
    @Mock private BakongProperties bakongProperties;
    @InjectMocks private ReceiptServiceImpl service;

    @Test
    void aCompletedPickupOrderGetsAReceipt() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.COMPLETED, LocalDateTime.now()));

        byte[] pdf = service.generateReceiptPdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void aDeliveredDeliveryOrderAlsoGetsAReceipt() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.DELIVERED, LocalDateTime.now()));

        byte[] pdf = service.generateReceiptPdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void anOrderStillInProgressHasNoReceiptYet() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.OUT_FOR_DELIVERY, LocalDateTime.now()));

        assertThatThrownBy(() -> service.generateReceiptPdf(orderId))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void aPaidOrderStillBeingPreparedAlreadyGetsAnInvoice() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.PREPARING, LocalDateTime.now()));

        byte[] pdf = service.generateInvoicePdf(orderId);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void anUnpaidOrderHasNoInvoiceYet() {
        UUID orderId = UUID.randomUUID();
        when(orderService.getAny(orderId)).thenReturn(order(OrderStatus.PENDING, null));

        assertThatThrownBy(() -> service.generateInvoicePdf(orderId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("hasn't been paid");
    }

    private OrderResponse order(OrderStatus status, LocalDateTime paidAt) {
        return new OrderResponse(
                UUID.randomUUID(), null, null, null, null, null,
                status, List.of(), new BigDecimal("10.00"),
                FulfillmentMethod.PICKUP, null, null, null, null, null,
                PaymentMethod.CASH, new BigDecimal("10.00"), Currency.USD, BigDecimal.ZERO, Currency.USD,
                null, null, null, null,
                null, paidAt, LocalDateTime.now(),
                null, null, null, null);
    }
}
