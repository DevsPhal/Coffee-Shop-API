package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.common.properties.ShopLocationProperties;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.order.mapper.OrderAuditLogMapper;
import org.group1.coffeeshopapi.order.mapper.OrderMapper;
import org.group1.coffeeshopapi.order.repository.OrderAuditLogRepository;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the cash-on-delivery/pickup real-business flow: a cash order can be prepared before its
 * cash is ever collected, so payment (paidAt) and fulfillment progress (OrderStatus) are no
 * longer the same signal. A Bakong order is unaffected — it still has to clear before prep starts,
 * since there's no in-person moment to fall back on. See OrderServiceImpl#startPreparing/markPaid.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplFulfillmentFlowTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderAuditLogRepository orderAuditLogRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductExtraRepository productExtraRepository;
    @Mock private ExtraRepository extraRepository;
    @Mock private InventoryService inventoryService;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderAuditLogMapper orderAuditLogMapper;
    @Mock private BakongQrService bakongQrService;
    @Mock private BakongApiClient bakongApiClient;
    @Mock private BakongExchangeRateService bakongExchangeRateService;
    @Mock private TelegramInvoiceService telegramInvoiceService;
    @Mock private CustomerRepository customerRepository;
    @Mock private ActorLookupService actorLookupService;
    @Mock private ShopLocationProperties shopLocationProperties;
    @Mock private BakongProperties bakongProperties;
    @InjectMocks private OrderServiceImpl service;

    @Test
    void startPreparingCutsStockAndClaimsAPendingCashOrderWithoutRequiringPaymentFirst() {
        UUID actorId = UUID.randomUUID();
        Order order = pendingCashOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.startPreparing(order.getId(), actorId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.getHandledBy()).isEqualTo(actorId);
        assertThat(order.getPaidAt()).isNull();
        verify(inventoryService).stockCut(any(StockCutRequest.class), eq(actorId));
    }

    @Test
    void startPreparingLeavesAnAlreadyClaimedCashOrdersHandledByUntouched() {
        UUID originalHandler = UUID.randomUUID();
        UUID differentActor = UUID.randomUUID();
        Order order = pendingCashOrder();
        order.setHandledBy(originalHandler);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.startPreparing(order.getId(), differentActor);

        assertThat(order.getHandledBy()).isEqualTo(originalHandler);
    }

    @Test
    void startPreparingRejectsAPendingBakongOrderThatHasNotClearedYet() {
        Order order = pendingCashOrder();
        order.setPaymentMethod(PaymentMethod.BAKONG);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.startPreparing(order.getId(), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not paid");
        verify(inventoryService, never()).stockCut(any(), any());
    }

    @Test
    void startPreparingStillAcceptsAnAlreadyPaidOrderAsBefore() {
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.startPreparing(order.getId(), UUID.randomUUID());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        // Stock was already cut back when the order reached PAID — starting prep must not cut it twice.
        verify(inventoryService, never()).stockCut(any(), any());
    }

    @Test
    void payingCashOnAnOrderAlreadyInPreparationSettlesPaymentWithoutReCuttingStockOrChangingStatus() {
        UUID barista = UUID.randomUUID();
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.PREPARING);
        order.setHandledBy(barista);
        when(orderRepository.findByIdAndHandledBy(order.getId(), barista)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), barista, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00")));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(order.getPaidAt()).isNotNull();
        verify(inventoryService, never()).stockCut(any(), any());
    }

    @Test
    void collectCashSettlesAnOutForDeliveryCashOrderWithoutRegressingItsStatus() {
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setFulfillmentMethod(FulfillmentMethod.DELIVERY);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.collectCash(order.getId(), UUID.randomUUID(),
                new CashPaymentRequest(Currency.USD, new BigDecimal("10.00")));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(order.getPaidAt()).isNotNull();
    }

    @Test
    void payCashRejectsAnOrderThatHasAlreadyBeenPaid() {
        UUID barista = UUID.randomUUID();
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.PREPARING);
        order.setHandledBy(barista);
        order.setPaidAt(LocalDateTime.now());
        when(orderRepository.findByIdAndHandledBy(order.getId(), barista)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.payCash(order.getId(), barista,
                new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already been collected");
    }

    @Test
    void payCashRejectsACancelledOrder() {
        UUID barista = UUID.randomUUID();
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.CANCELLED);
        order.setHandledBy(barista);
        when(orderRepository.findByIdAndHandledBy(order.getId(), barista)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.payCash(order.getId(), barista,
                new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void markDeliveredRejectsAnUnpaidCashOrderEvenThoughItHasArrived() {
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setFulfillmentMethod(FulfillmentMethod.DELIVERY);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.markDelivered(order.getId(), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Collect payment");
    }

    @Test
    void markDeliveredSucceedsOnceCashHasBeenCollected() {
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setFulfillmentMethod(FulfillmentMethod.DELIVERY);
        order.setPaidAt(LocalDateTime.now());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markDelivered(order.getId(), UUID.randomUUID());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void completePickupRejectsAnUnpaidCashOrderEvenThoughItsReady() {
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.completePickup(order.getId(), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Collect payment");
    }

    @Test
    void completePickupSucceedsOnceCashHasBeenCollected() {
        Order order = pendingCashOrder();
        order.setStatus(OrderStatus.PREPARING);
        order.setPaidAt(LocalDateTime.now());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.completePickup(order.getId(), UUID.randomUUID());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void listAwaitingPreparationIncludesPendingCashOrdersAlongsidePaidOnes() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.unpaged();
        when(orderRepository.findAwaitingPreparation(OrderStatus.PAID, OrderStatus.PENDING, PaymentMethod.CASH, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.listAwaitingPreparation(pageable);

        verify(orderRepository).findAwaitingPreparation(OrderStatus.PAID, OrderStatus.PENDING, PaymentMethod.CASH, pageable);
    }

    private Order pendingCashOrder() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setTotalAmount(new BigDecimal("10.00"));
        order.addItem(item);
        return order;
    }
}
