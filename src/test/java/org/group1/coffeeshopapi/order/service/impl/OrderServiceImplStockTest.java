package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
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
import org.group1.coffeeshopapi.realtime.ResourceChangePublisher;
import org.group1.coffeeshopapi.realtime.event.OrderChangedEvent;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.user.entity.Customer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Stock is counted in stock units (cartons), sales in sell units (cans); stock is checked before
// any money changes hands, and a Bakong payment that already arrived is never blocked by it.
@ExtendWith(MockitoExtension.class)
class OrderServiceImplStockTest {

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
    void payingCutsStockInStockUnitsNotSellUnits() {
        UUID barista = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 3);
        order.setHandledBy(barista);
        when(orderRepository.findByHandledByForUpdate(order.getId(), barista)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), barista, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        ArgumentCaptor<StockCutRequest> cut = ArgumentCaptor.forClass(StockCutRequest.class);
        verify(inventoryService).stockCut(cut.capture(), eq(barista));
        assertThat(cut.getValue().quantity()).isEqualByComparingTo("0.125");
    }

    @Test
    void placingAnOrderChecksStockForAllLinesOfTheSameProductTogether() {
        Product cans = cartonOf24Cans();
        ProductVariant piece = new ProductVariant();
        piece.setId(UUID.randomUUID());
        piece.setName(VariantLabel.MEDIUM);
        piece.setPrice(new BigDecimal("0.50"));
        piece.setStatus(Status.ACTIVE);
        when(productRepository.findById(cans.getId())).thenReturn(Optional.of(cans));
        when(variantRepository.findByIdAndProductId(piece.getId(), cans.getId())).thenReturn(Optional.of(piece));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new StaffCreateOrderRequest(List.of(
                new OrderItemRequest(cans.getId(), 12, piece.getId(), null, null, null, null, List.of()),
                new OrderItemRequest(cans.getId(), 12, piece.getId(), null, null, null, null, List.of())), null),
                UUID.randomUUID());

        // 12 + 12 cans = one whole carton, checked once.
        verify(inventoryService).requireAvailable(cans.getId(), new BigDecimal("1.000"));
    }

    @Test
    void aSoldOutProductIsRefusedBeforeTheCustomerCanPay() {
        UUID customerId = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        doThrow(new InvalidOperationException("'Soda Can' doesn't have enough stock left for this order"))
                .when(inventoryService).requireAvailable(any(), any());

        assertThatThrownBy(() -> service.generateBakongQrForCustomer(order.getId(), customerId, Currency.USD))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("enough stock");
        verify(bakongQrService, never()).generateQr(any(), any(), any());
    }

    @Test
    void aConfirmedBakongPaymentIsRecordedEvenIfStockRanShort() {
        UUID customerId = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongMd5Hash("md5");
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bakongApiClient.checkTransactionByMd5("md5")).thenReturn(BakongTransactionCheckResult.paid("tx", new BigDecimal("1.50"), "USD", null));

        service.confirmBakongPaymentForCustomer(order.getId(), customerId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(inventoryService).stockCutAvailable(any(StockCutRequest.class), any());
        verify(inventoryService, never()).stockCut(any(), any());
    }

    private Product cartonOf24Cans() {
        Category drinks = new Category();
        drinks.setStatus(Status.ACTIVE);
        drinks.setCategoryGroup(CategoryGroup.BEVERAGE);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Soda Can");
        product.setUnitsPerStock(new BigDecimal("24"));
        product.setCategory(drinks);
        return product;
    }

    private Order pendingOrder(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setSubtotal(new BigDecimal("1.50"));

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setTotalAmount(new BigDecimal("1.50"));
        order.addItem(item);
        return order;
    }
}
