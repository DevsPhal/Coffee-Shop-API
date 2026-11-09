package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.common.enums.OrderType;
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
import org.group1.coffeeshopapi.inventory.dto.request.SaleLine;
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
    void payingCutsJustTheCansSoldNotAPartOfACarton() {
        UUID barista = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 3);
        order.setHandledBy(barista);
        when(orderRepository.findByHandledByForUpdate(order.getId(), barista)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), barista, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        verify(inventoryService).deductForSale(eq(order.getId()),
                eq(List.of(new SaleLine(order.getItems().get(0).getProduct().getId(), null, 1, 3))), eq(barista), eq(false));
    }

    @Test
    void payingForTwoFullCartonsCutsTheFortyEightCansInThem() {
        UUID barista = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 2);
        ProductVariant carton = variant(VariantLabel.CARTON, "16.00", 24);
        order.getItems().get(0).setVariant(carton);
        order.setHandledBy(barista);
        when(orderRepository.findByHandledByForUpdate(order.getId(), barista)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.payCash(order.getId(), barista, new CashPaymentRequest(Currency.USD, new BigDecimal("10.00"), null));

        verify(inventoryService).deductForSale(eq(order.getId()), eq(List.of(new SaleLine(
                order.getItems().get(0).getProduct().getId(), carton.getId(), 24, 2))), eq(barista), eq(false));
    }

    @Test
    void placingAnOrderChecksStockForAllLinesOfTheSameProductTogether() {
        Product cans = cartonOf24Cans();
        ProductVariant piece = variant(VariantLabel.MEDIUM, "0.50", 1);
        when(productRepository.findById(cans.getId())).thenReturn(Optional.of(cans));
        when(variantRepository.findByIdAndProductId(piece.getId(), cans.getId())).thenReturn(Optional.of(piece));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new StaffCreateOrderRequest(List.of(
                new OrderItemRequest(cans.getId(), 12, piece.getId(), null, null, null, null, List.of()),
                new OrderItemRequest(cans.getId(), 12, piece.getId(), null, null, null, null, List.of())), null, OrderType.TAKE_AWAY, null),
                UUID.randomUUID());

        verify(inventoryService).requireAvailableForSale(List.of(
                new SaleLine(cans.getId(), piece.getId(), 1, 12), new SaleLine(cans.getId(), piece.getId(), 1, 12)));
    }

    @Test
    void aCartonAndLooseCansOfTheSameDrinkNeedTheirCansCombined() {
        Product cans = cartonOf24Cans();
        ProductVariant single = variant(VariantLabel.MEDIUM, "0.75", 1);
        ProductVariant carton = variant(VariantLabel.CARTON, "16.00", 24);
        when(productRepository.findById(cans.getId())).thenReturn(Optional.of(cans));
        when(variantRepository.findByIdAndProductId(single.getId(), cans.getId())).thenReturn(Optional.of(single));
        when(variantRepository.findByIdAndProductId(carton.getId(), cans.getId())).thenReturn(Optional.of(carton));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new StaffCreateOrderRequest(List.of(
                new OrderItemRequest(cans.getId(), 1, carton.getId(), null, null, null, null, List.of()),
                new OrderItemRequest(cans.getId(), 3, single.getId(), null, null, null, null, List.of())), null, OrderType.TAKE_AWAY, null),
                UUID.randomUUID());

        verify(inventoryService).requireAvailableForSale(List.of(
                new SaleLine(cans.getId(), carton.getId(), 24, 1), new SaleLine(cans.getId(), single.getId(), 1, 3)));
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
                .when(inventoryService).requireAvailableForSale(any());

        assertThatThrownBy(() -> service.generateBakongQrForCustomer(order.getId(), customerId, Currency.USD))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("enough stock");
        verify(bakongQrService, never()).generateQr(any(), any(), any(), any());
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
        verify(inventoryService).deductForSale(eq(order.getId()), any(), any(), eq(true));
    }

    @Test
    void aPaymentToAReplacedQrIsStillConfirmed() {
        UUID customerId = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongMd5Hash("khr-md5");
        order.setBakongCurrency(Currency.KHR);
        long stillPayable = java.time.Instant.now().plusSeconds(600).getEpochSecond();
        order.setBakongPreviousMd5Hashes("usd-md5@" + stillPayable);
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bakongApiClient.checkTransactionByMd5("khr-md5")).thenReturn(BakongTransactionCheckResult.notPaid("not found"));
        when(bakongApiClient.checkTransactionByMd5("usd-md5")).thenReturn(BakongTransactionCheckResult.paid("tx", new BigDecimal("1.50"), "USD", null));

        service.confirmBakongPaymentForCustomer(order.getId(), customerId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getBakongMd5Hash()).isEqualTo("usd-md5");
        assertThat(order.getBakongCurrency()).isEqualTo(Currency.USD);
        assertThat(order.getBakongTransactionHash()).isEqualTo("tx");
    }

    @Test
    void anExpiredReplacedQrIsNotCheckedAgain() {
        UUID customerId = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongMd5Hash("current");
        long longGone = java.time.Instant.now().minusSeconds(24 * 3600).getEpochSecond();
        order.setBakongPreviousMd5Hashes("stale@" + longGone);
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(bakongApiClient.checkTransactionByMd5("current")).thenReturn(BakongTransactionCheckResult.notPaid("not found"));

        service.confirmBakongPaymentForCustomer(order.getId(), customerId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(bakongApiClient, never()).checkTransactionByMd5("stale");
    }

    @Test
    void aStillValidQrIsHandedBackInsteadOfReplaced() {
        UUID customerId = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongQrString("qr");
        order.setBakongMd5Hash("live");
        order.setBakongCurrency(Currency.USD);
        order.setBakongAmount(new BigDecimal("1.50"));
        order.setBakongExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));

        var response = service.generateBakongQrForCustomer(order.getId(), customerId, Currency.USD);

        assertThat(response.md5Hash()).isEqualTo("live");
        assertThat(response.expiresInSeconds()).isBetween(500L, 600L);
        verify(bakongQrService, never()).generateQr(any(), any(), any(), any());
    }

    @Test
    void replacingAQrKeepsTheOldHashCheckable() {
        UUID customerId = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongQrString("usd-qr");
        order.setBakongMd5Hash("usd-md5");
        order.setBakongCurrency(Currency.USD);
        order.setBakongExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bakongQrService.generateQr(any(), any(), eq(Currency.KHR), any())).thenReturn(new org.group1.coffeeshopapi.bakong.dto.BakongQrResult(
                "khr-qr", "khr-md5", Currency.KHR, new BigDecimal("6150"), LocalDateTime.now().plusMinutes(15)));

        service.generateBakongQrForCustomer(order.getId(), customerId, Currency.KHR);

        assertThat(order.getBakongMd5Hash()).isEqualTo("khr-md5");
        assertThat(order.getBakongPreviousMd5Hashes()).startsWith("usd-md5@");
    }

    @Test
    void aBlockedBakongCheckIsReportedInsteadOfNotPaid() {
        UUID customerId = UUID.randomUUID();
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongMd5Hash("md5");
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(bakongApiClient.checkTransactionByMd5("md5")).thenReturn(BakongTransactionCheckResult.failed("Bakong refused the payment check (403)."));

        assertThatThrownBy(() -> service.confirmBakongPaymentForCustomer(order.getId(), customerId))
                .isInstanceOf(org.group1.coffeeshopapi.common.exception.PaymentVerificationUnavailableException.class)
                .hasMessageContaining("403");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void switchingCurrencyKeepsTheOrdersPaymentDeadline() {
        UUID customerId = UUID.randomUUID();
        Order order = expiringBakongOrder(customerId, LocalDateTime.now().plusMinutes(10));
        LocalDateTime deadline = order.getBakongExpiresAt();
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bakongQrService.generateQr(any(), any(), eq(Currency.KHR), eq(deadline))).thenReturn(
                new org.group1.coffeeshopapi.bakong.dto.BakongQrResult(
                        "khr-qr", "khr-md5", Currency.KHR, new BigDecimal("6150"), deadline));

        service.generateBakongQrForCustomer(order.getId(), customerId, Currency.KHR);

        assertThat(order.getBakongExpiresAt()).isEqualTo(deadline);
    }

    @Test
    void noNewQrOnceThePaymentTimeHasRunOut() {
        UUID customerId = UUID.randomUUID();
        Order order = expiringBakongOrder(customerId, LocalDateTime.now().minusSeconds(5));
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.generateBakongQrForCustomer(order.getId(), customerId, Currency.KHR))
                .hasMessageContaining("run out");
        verify(bakongQrService, never()).generateQr(any(), any(), any(), any());
    }

    @Test
    void anExpiredUnpaidQrOrderIsCancelled() {
        Order order = expiringBakongOrder(UUID.randomUUID(), LocalDateTime.now().minusMinutes(2));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bakongApiClient.checkTransactionByMd5("usd-md5", true)).thenReturn(BakongTransactionCheckResult.notPaid("not found"));

        assertThat(service.cancelExpiredBakongOrder(order.getId())).isTrue();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void anExpiredQrOrderPaidJustInTimeIsConfirmedInsteadOfCancelled() {
        Order order = expiringBakongOrder(UUID.randomUUID(), LocalDateTime.now().minusMinutes(2));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bakongApiClient.checkTransactionByMd5("usd-md5", true))
                .thenReturn(BakongTransactionCheckResult.paid("tx", new BigDecimal("1.50"), "USD", null));

        assertThat(service.cancelExpiredBakongOrder(order.getId())).isFalse();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void anExpiredQrOrderIsNotCancelledWhileBakongCannotBeChecked() {
        Order order = expiringBakongOrder(UUID.randomUUID(), LocalDateTime.now().minusMinutes(2));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(bakongApiClient.checkTransactionByMd5("usd-md5", true))
                .thenReturn(BakongTransactionCheckResult.failed("daily limit"));

        assertThatThrownBy(() -> service.cancelExpiredBakongOrder(order.getId()))
                .isInstanceOf(org.group1.coffeeshopapi.common.exception.PaymentVerificationUnavailableException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    private Order expiringBakongOrder(UUID customerId, LocalDateTime expiresAt) {
        Order order = pendingOrder(cartonOf24Cans(), 1);
        Customer customer = new Customer();
        customer.setId(customerId);
        order.setCustomer(customer);
        order.setPaymentMethod(PaymentMethod.BAKONG);
        order.setBakongQrString("usd-qr");
        order.setBakongMd5Hash("usd-md5");
        order.setBakongCurrency(Currency.USD);
        order.setBakongExpiresAt(expiresAt);
        return order;
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

    private ProductVariant variant(VariantLabel label, String price, int unitsPerSale) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setName(label);
        variant.setPrice(new BigDecimal(price));
        variant.setUnitsPerSale(unitsPerSale);
        variant.setStatus(Status.ACTIVE);
        return variant;
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
