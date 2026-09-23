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
import org.group1.coffeeshopapi.realtime.ChangeType;
import org.group1.coffeeshopapi.realtime.ResourceChangePublisher;
import org.group1.coffeeshopapi.realtime.ResourceType;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers the extra-stock deduction that happens on top of the regular product stock cut when
// an order is paid.
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
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ResourceChangePublisher resourceChangePublisher;
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
        when(orderRepository.findByHandledByForUpdate(order.getId(), baristaId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        verify(extraRepository).deductStock(pearl.getId(), new BigDecimal("2"));
        verify(resourceChangePublisher).record(ResourceType.EXTRA, pearl.getId(), ChangeType.UPDATED);
    }

    @Test
    void payingCashDeductsTheFullSoldQuantityAndLeavesTheZeroFloorToTheDatabase() {
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
        when(orderRepository.findByHandledByForUpdate(order.getId(), baristaId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        verify(extraRepository).deductStock(pearl.getId(), new BigDecimal("3"));
    }

    @Test
    void payingCashLeavesAnUntrackedExtraAloneAndNeverSavesIt() {
        Extra untracked = new Extra();
        untracked.setId(UUID.randomUUID());
        // Left null on purpose — null means this extra's stock isn't tracked.

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
        when(orderRepository.findByHandledByForUpdate(order.getId(), baristaId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        verify(extraRepository, never()).deductStock(any(), any());
        assertThat(untracked.getQuantityOnHand()).isNull();
    }
}
