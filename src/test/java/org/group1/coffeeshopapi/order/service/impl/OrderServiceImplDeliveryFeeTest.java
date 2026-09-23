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
import org.group1.coffeeshopapi.order.dto.request.CheckoutDetailsRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.DeliveryLocationRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers the cash-on-delivery/pickup flow: a cash order can start being prepared before its
// cash is actually collected, so payment and fulfillment progress aren't the same signal.
// A delivery order's total isn't final until staff quotes the fee from the pinned distance, so
// paying or preparing it is blocked until then.
@ExtendWith(MockitoExtension.class)
class OrderServiceImplDeliveryFeeTest {

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

    private final UUID customerId = UUID.randomUUID();

    @Test
    void pinningALocationMakesItADeliveryOrderAwaitingAFreshQuote() {
        Order order = customerOrder();
        order.setDeliveryFee(new BigDecimal("1.00"));
        order.setDeliveryFeeSetAt(LocalDateTime.now());
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setBakongMd5Hash("old");
        stubCustomerLookup(order);

        service.pinDeliveryLocation(order.getId(), customerId, location());

        assertThat(order.getFulfillmentMethod()).isEqualTo(FulfillmentMethod.DELIVERY);
        assertThat(order.isAwaitingDeliveryFee()).isTrue();
        assertThat(order.getDeliveryFee()).isEqualByComparingTo("0");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("10.00");
        assertThat(order.getPaymentMethod()).isNull();
        assertThat(order.getBakongMd5Hash()).isNull();
    }

    @Test
    void customerCannotPayBeforeTheFeeIsQuoted() {
        Order order = awaitingQuote();
        stubCustomerLookup(order);

        assertThatThrownBy(() -> service.selectCashOnPickup(order.getId(), customerId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("delivery fee");
        assertThatThrownBy(() -> service.generateBakongQrForCustomer(order.getId(), customerId, Currency.USD))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("delivery fee");
    }

    @Test
    void staffCannotStartPreparingBeforeTheFeeIsQuoted() {
        Order order = awaitingQuote();
        order.setPaymentMethod(PaymentMethod.CASH);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.startPreparing(order.getId(), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class);
        verify(inventoryService, never()).stockCut(any(), any());
    }

    @Test
    void quotingTheFeeAddsItToTheGrandTotalAndUnblocksPayment() {
        Order order = awaitingQuote();
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        stubCustomerLookup(order);

        service.setDeliveryFee(order.getId(), new BigDecimal("1.50"), UUID.randomUUID());

        assertThat(order.isAwaitingDeliveryFee()).isFalse();
        assertThat(order.getItemsTotal()).isEqualByComparingTo("10.00");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("11.50");
        service.selectCashOnPickup(order.getId(), customerId);
        assertThat(order.getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void aQuotedFreeDeliveryIsNotStillAwaiting() {
        Order order = awaitingQuote();
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.setDeliveryFee(order.getId(), BigDecimal.ZERO, UUID.randomUUID());

        assertThat(order.isAwaitingDeliveryFee()).isFalse();
    }

    @Test
    void checkoutForDeliveryNeedsAPinAndAPhone() {
        CheckoutDetailsRequest noPhone = new CheckoutDetailsRequest(
                FulfillmentMethod.DELIVERY, "Dara", null, "St 590, Phnom Penh");
        CheckoutDetailsRequest complete = new CheckoutDetailsRequest(
                FulfillmentMethod.DELIVERY, "Dara", "012345678", "St 590, Phnom Penh");

        assertThatThrownBy(() -> service.createForCustomer(
                new CreateOrderRequest(List.of(), null, complete), customerId, null, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Pin");
        assertThatThrownBy(() -> service.createForCustomer(
                new CreateOrderRequest(List.of(), null, noPhone), customerId,
                new BigDecimal("11.55"), new BigDecimal("104.92")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("phone");
    }

    private void stubCustomerLookup(Order order) {
        when(orderRepository.findByCustomerForUpdate(order.getId(), customerId)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private DeliveryLocationRequest location() {
        return new DeliveryLocationRequest(new BigDecimal("11.556"), new BigDecimal("104.928"),
                "St 590, Phnom Penh", "Dara", "012345678");
    }

    private Order awaitingQuote() {
        Order order = customerOrder();
        order.setFulfillmentMethod(FulfillmentMethod.DELIVERY);
        order.setDeliveryLatitude(new BigDecimal("11.556"));
        order.setDeliveryLongitude(new BigDecimal("104.928"));
        return order;
    }

    private Order customerOrder() {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setEmail("customer@example.com");

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Iced Green Tea");

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("10.00"));

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("10.00"));
        order.addItem(item);
        return order;
    }
}
