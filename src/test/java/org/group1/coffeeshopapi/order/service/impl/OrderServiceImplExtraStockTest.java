package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.common.properties.ShopLocationProperties;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.order.entity.OrderItemExtra;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused on the new extra-stock deduction {@code markPaid} does on top of the existing product
 * stockCut — not a general OrderServiceImpl test (the class has far too much surface for that
 * here). See Extra.quantityOnHand / ProductExtraResolver for the rest of this feature.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplExtraStockTest {

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
    void payingCashDecrementsEachSoldItemsExtrasStockByTheItemQuantity() {
        Extra pearl = new Extra();
        pearl.setId(UUID.randomUUID());
        pearl.setName("Pearl");
        pearl.setQuantityOnHand(new BigDecimal("5"));

        OrderItemExtra orderItemExtra = new OrderItemExtra();
        orderItemExtra.setExtra(pearl);

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.addExtra(orderItemExtra);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("10.00"));
        order.addItem(item);

        UUID baristaId = UUID.randomUUID();
        when(orderRepository.findByIdAndHandledBy(order.getId(), baristaId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        ArgumentCaptor<Extra> saved = ArgumentCaptor.forClass(Extra.class);
        verify(extraRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantityOnHand()).isEqualByComparingTo("3");
    }

    @Test
    void payingCashNeverGoesBelowZeroEvenIfMoreWasSoldThanTracked() {
        Extra pearl = new Extra();
        pearl.setId(UUID.randomUUID());
        pearl.setQuantityOnHand(new BigDecimal("1"));

        OrderItemExtra orderItemExtra = new OrderItemExtra();
        orderItemExtra.setExtra(pearl);

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(3);
        item.addExtra(orderItemExtra);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("10.00"));
        order.addItem(item);

        UUID baristaId = UUID.randomUUID();
        when(orderRepository.findByIdAndHandledBy(order.getId(), baristaId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        ArgumentCaptor<Extra> saved = ArgumentCaptor.forClass(Extra.class);
        verify(extraRepository).save(saved.capture());
        assertThat(saved.getValue().getQuantityOnHand()).isEqualByComparingTo("0");
    }

    @Test
    void payingCashLeavesAnUntrackedExtraAloneAndNeverSavesIt() {
        Extra untracked = new Extra();
        untracked.setId(UUID.randomUUID());
        // quantityOnHand left null on purpose — see Extra.quantityOnHand.

        OrderItemExtra orderItemExtra = new OrderItemExtra();
        orderItemExtra.setExtra(untracked);

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.addExtra(orderItemExtra);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("10.00"));
        order.addItem(item);

        UUID baristaId = UUID.randomUUID();
        when(orderRepository.findByIdAndHandledBy(order.getId(), baristaId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        verify(extraRepository, never()).save(any());
        assertThat(untracked.getQuantityOnHand()).isNull();
    }
}
