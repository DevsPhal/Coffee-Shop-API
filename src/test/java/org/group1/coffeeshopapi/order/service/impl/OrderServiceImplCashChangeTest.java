package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.common.properties.ShopLocationProperties;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.mapper.OrderAuditLogMapper;
import org.group1.coffeeshopapi.order.mapper.OrderMapper;
import org.group1.coffeeshopapi.order.repository.OrderAuditLogRepository;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Change is given back in whichever currency the customer wants — normally whatever they
// tendered in, but they can ask for the other one instead.
@ExtendWith(MockitoExtension.class)
class OrderServiceImplCashChangeTest {

    private static final BigDecimal USD_TO_KHR_RATE = new BigDecimal("4100");

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
    @InjectMocks private OrderServiceImpl service;

    @Test
    void tenderingUsdWithNoOverrideGivesChangeBackInUsd() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "10.00");

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("15.00"), null));

        assertThat(order.getChangeCurrency()).isEqualTo(Currency.USD);
        assertThat(order.getChangeDue()).isEqualByComparingTo("5.00");
    }

    @Test
    void tenderingKhrWithNoOverrideGivesChangeBackInKhrTooNotUsd() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "10.00");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(USD_TO_KHR_RATE);

        // 45,000 KHR ≈ $10.98 at this rate, so $0.98 change ≈ 4018 KHR.
        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.KHR, new BigDecimal("45000"), null));

        assertThat(order.getChangeCurrency()).isEqualTo(Currency.KHR);
        assertThat(order.getChangeDue()).isEqualByComparingTo("4018");
    }

    @Test
    void customerCanTenderKhrButAskForChangeBackInUsd() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "10.00");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(USD_TO_KHR_RATE);

        service.payCash(order.getId(), baristaId,
                new CashPaymentRequest(Currency.KHR, new BigDecimal("45000"), Currency.USD));

        assertThat(order.getChangeCurrency()).isEqualTo(Currency.USD);
        assertThat(order.getChangeDue()).isEqualByComparingTo("0.98");
    }

    @Test
    void customerCanTenderUsdButAskForChangeBackInKhr() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "10.00");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(USD_TO_KHR_RATE);

        service.payCash(order.getId(), baristaId,
                new CashPaymentRequest(Currency.USD, new BigDecimal("15.00"), Currency.KHR));

        assertThat(order.getChangeCurrency()).isEqualTo(Currency.KHR);
        // $5.00 change * 4100 = 20500 KHR.
        assertThat(order.getChangeDue()).isEqualByComparingTo("20500");
    }

    @Test
    void askingForKhrChangeWithNoExchangeRateConfiguredFailsClearly() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "10.00");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(null);

        assertThatThrownBy(() -> service.payCash(order.getId(), baristaId,
                new CashPaymentRequest(Currency.USD, new BigDecimal("15.00"), Currency.KHR)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exchange rate");
    }

    private Order pendingOrder(UUID baristaId, String totalAmount) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal(totalAmount));
        when(orderRepository.findByHandledByForUpdate(order.getId(), baristaId)).thenReturn(Optional.of(order));
        // Not every test in this class reaches save() (the exchange-rate-missing case throws
        // first), so this stub is lenient rather than required.
        Mockito.lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return order;
    }
}
