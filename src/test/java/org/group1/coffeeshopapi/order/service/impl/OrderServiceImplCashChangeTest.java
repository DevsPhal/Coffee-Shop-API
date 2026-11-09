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
import org.group1.coffeeshopapi.realtime.ResourceChangePublisher;
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
    @Mock private ResourceChangePublisher resourceChangePublisher;
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

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.KHR, new BigDecimal("45000"), null));

        // $10.00 at 4,100 is 41,000 riel, so 45,000 riel gives exactly 4,000 back (not 4,018 from a round trip
        // through dollars).
        assertThat(order.getChangeCurrency()).isEqualTo(Currency.KHR);
        assertThat(order.getChangeDue()).isEqualByComparingTo("4000");
        assertThat(order.getExchangeRate()).isEqualByComparingTo("4100");
    }

    @Test
    void rielTotalThatRoundsDownIsEnoughToPay() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "0.30");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(USD_TO_KHR_RATE);

        // $0.30 is 1,230 riel, which rounds down to 1,200: paying 1,200 riel is the full amount.
        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.KHR, new BigDecimal("1200"), null));

        assertThat(order.getChangeDue()).isEqualByComparingTo("0");
        assertThat(order.getAmountTendered()).isEqualByComparingTo("1200");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.30");
    }

    @Test
    void rielBelowTheRoundedTotalIsRefusedWithTheRielAmountDue() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "1.19");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(USD_TO_KHR_RATE);

        // $1.19 is 4,879 riel, which rounds up to 4,900: 4,879 riel is not enough.
        assertThatThrownBy(() -> service.payCash(order.getId(), baristaId,
                new CashPaymentRequest(Currency.KHR, new BigDecimal("4879"), null)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("4,900 KHR");
    }

    @Test
    void rielChangeComesFromTheRoundedRielTotal() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "1.19");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(USD_TO_KHR_RATE);

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.KHR, new BigDecimal("10000"), null));

        assertThat(order.getChangeDue()).isEqualByComparingTo("5100");
    }

    @Test
    void dollarChangeGivenInRielIsRoundedToTheNearest100() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "1.19");
        when(bakongExchangeRateService.getCurrentRate()).thenReturn(USD_TO_KHR_RATE);

        // $3.81 change is 15,621 riel; the last two digits (21) round down to 15,600.
        service.payCash(order.getId(), baristaId,
                new CashPaymentRequest(Currency.USD, new BigDecimal("5.00"), Currency.KHR));

        assertThat(order.getChangeDue()).isEqualByComparingTo("15600");
        assertThat(order.getChangeCurrency()).isEqualTo(Currency.KHR);
    }

    @Test
    void dollarsOnlyKeepsCentsAndRecordsNoRate() {
        UUID baristaId = UUID.randomUUID();
        Order order = pendingOrder(baristaId, "1.19");

        service.payCash(order.getId(), baristaId, new CashPaymentRequest(Currency.USD, new BigDecimal("2.00"), null));

        assertThat(order.getChangeDue()).isEqualByComparingTo("0.81");
        assertThat(order.getExchangeRate()).isNull();
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
        Mockito.lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return order;
    }
}
