package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.StaffCallReason;
import org.group1.coffeeshopapi.common.enums.StaffCallStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.TooManyRequestsException;
import org.group1.coffeeshopapi.order.dto.request.StaffCallRequest;
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderAuditLog;
import org.group1.coffeeshopapi.order.repository.OrderAuditLogRepository;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.realtime.dto.StaffCallMessage;
import org.group1.coffeeshopapi.realtime.event.StaffCallEvent;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class StaffCallServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderAuditLogRepository orderAuditLogRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ActorLookupService actorLookupService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private HashOperations<String, Object, Object> hashOps;
    @InjectMocks private StaffCallServiceImpl service;

    private final UUID customerId = UUID.randomUUID();
    private final StaffCallRequest paymentHelp = new StaffCallRequest(StaffCallReason.PAYMENT_HELP, null);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "cooldownSeconds", 30L);
        ReflectionTestUtils.setField(service, "replyVisibleMinutes", 60L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.<Object, Object>opsForHash()).thenReturn(hashOps);
    }

    @Test
    void theFirstCallAlertsStaffAndStartsThe30SecondCooldown() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(30)))).thenReturn(true);

        StaffCallResponse response = service.call(order.getId(), customerId, paymentHelp);

        assertThat(response.nextCallAllowedAt()).isAfter(LocalDateTime.now().plusSeconds(25));
        ArgumentCaptor<StaffCallEvent> event = ArgumentCaptor.forClass(StaffCallEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().message().type()).isEqualTo(StaffCallMessage.Type.CALLED);
        assertThat(event.getValue().message().customerName()).isEqualTo("Customer Luku");
        assertThat(event.getValue().message().reason()).isEqualTo(StaffCallReason.PAYMENT_HELP);
        assertThat(response.reason()).isEqualTo(StaffCallReason.PAYMENT_HELP);
        ArgumentCaptor<OrderAuditLog> log = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(orderAuditLogRepository).save(log.capture());
        assertThat(log.getValue().getAction()).isEqualTo(OrderAuditAction.STAFF_CALLED);
    }

    @Test
    void callingAgainWithinTheCooldownIsRefusedWithTheSecondsLeft() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(redisTemplate.getExpire(RedisKeys.STAFF_CALL_COOLDOWN_PREFIX + customerId)).thenReturn(18L);

        assertThatThrownBy(() -> service.call(order.getId(), customerId, paymentHelp))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("18 seconds");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void staffCantBeCalledForAFinishedOrder() {
        Order order = order(OrderStatus.COMPLETED);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.call(order.getId(), customerId, paymentHelp))
                .isInstanceOf(InvalidOperationException.class);
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void answeringClearsTheCallAndSendsTheReplyToTheCustomer() {
        Order order = order(OrderStatus.PENDING);
        UUID baristaId = UUID.randomUUID();
        String answeredKey = RedisKeys.STAFF_CALL_ANSWERED_PREFIX + order.getId();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(hashOps.get(RedisKeys.STAFF_CALL_OPEN, order.getId().toString()))
                .thenReturn(LocalDateTime.now().minusMinutes(1) + "|WRONG_OR_MISSING_ITEM|No straw");
        when(hashOps.delete(RedisKeys.STAFF_CALL_OPEN, order.getId().toString())).thenReturn(1L);
        when(actorLookupService.resolve(baristaId)).thenReturn(new ActorSummary(baristaId, "Barista Dara", null));

        StaffCallResponse response = service.answer(order.getId(), baristaId, "  On my way with a straw  ");

        assertThat(response.status()).isEqualTo(StaffCallStatus.ANSWERED);
        assertThat(response.reply()).isEqualTo("On my way with a straw");
        assertThat(response.answeredByName()).isEqualTo("Barista Dara");
        ArgumentCaptor<StaffCallEvent> event = ArgumentCaptor.forClass(StaffCallEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().message().type()).isEqualTo(StaffCallMessage.Type.ANSWERED);
        assertThat(event.getValue().message().answeredByName()).isEqualTo("Barista Dara");
        assertThat(event.getValue().message().reply()).isEqualTo("On my way with a straw");
        assertThat(event.getValue().customerEmail()).isEqualTo("luku@example.com");
        assertThat(event.getValue().message().reason()).isEqualTo(StaffCallReason.WRONG_OR_MISSING_ITEM);
        ArgumentCaptor<Map<String, String>> stored = ArgumentCaptor.forClass(Map.class);
        verify(hashOps).putAll(eq(answeredKey), stored.capture());
        assertThat(stored.getValue()).containsEntry("reply", "On my way with a straw")
                .containsEntry("answeredByName", "Barista Dara");
        verify(redisTemplate).expire(answeredKey, Duration.ofMinutes(60));
    }

    @Test
    void theCustomerCanSeeTheHandlersReplyAfterTheCallIsAnswered() {
        Order order = order(OrderStatus.PREPARING);
        LocalDateTime calledAt = LocalDateTime.now().minusMinutes(3);
        LocalDateTime answeredAt = LocalDateTime.now().minusMinutes(1);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(hashOps.entries(RedisKeys.STAFF_CALL_ANSWERED_PREFIX + order.getId())).thenReturn(Map.of(
                "calledAt", calledAt.toString(), "reason", "PAYMENT_HELP",
                "answeredAt", answeredAt.toString(), "answeredByName", "Barista Dara",
                "reply", "Please come to the counter"));

        StaffCallResponse response = service.current(order.getId(), customerId);

        assertThat(response.status()).isEqualTo(StaffCallStatus.ANSWERED);
        assertThat(response.reason()).isEqualTo(StaffCallReason.PAYMENT_HELP);
        assertThat(response.reply()).isEqualTo("Please come to the counter");
        assertThat(response.answeredAt()).isEqualTo(answeredAt);
    }

    @Test
    void theCustomerSeesTheirOpenCallWithTheCooldownLeft() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(redisTemplate.getExpire(RedisKeys.STAFF_CALL_COOLDOWN_PREFIX + customerId)).thenReturn(12L);
        when(hashOps.get(RedisKeys.STAFF_CALL_OPEN, order.getId().toString()))
                .thenReturn(LocalDateTime.now() + "|ORDER_DELAY|");

        StaffCallResponse response = service.current(order.getId(), customerId);

        assertThat(response.status()).isEqualTo(StaffCallStatus.OPEN);
        assertThat(response.reply()).isNull();
        assertThat(response.nextCallAllowedAt()).isAfter(LocalDateTime.now().plusSeconds(8));
    }

    @Test
    void thereIsNothingToShowWhenStaffWasNeverCalled() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(hashOps.entries(RedisKeys.STAFF_CALL_ANSWERED_PREFIX + order.getId())).thenReturn(Map.of());

        assertThat(service.current(order.getId(), customerId)).isNull();
    }

    @Test
    void callingAgainClearsThePreviousReply() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        service.call(order.getId(), customerId, paymentHelp);

        verify(redisTemplate).delete(RedisKeys.STAFF_CALL_ANSWERED_PREFIX + order.getId());
    }

    @Test
    void otherNeedsANoteSayingWhatTheCustomerNeeds() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.call(order.getId(), customerId, new StaffCallRequest(StaffCallReason.OTHER, "  ")))
                .isInstanceOf(InvalidOperationException.class);
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void deliveryHelpIsOnlyForDeliveryOrders() {
        Order order = order(OrderStatus.PENDING);
        order.setFulfillmentMethod(FulfillmentMethod.PICKUP);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.call(order.getId(), customerId,
                new StaffCallRequest(StaffCallReason.DELIVERY_HELP, null)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void callingAgainKeepsTheOriginalCallTimeButUpdatesTheReason() {
        Order order = order(OrderStatus.PREPARING);
        LocalDateTime firstCalledAt = LocalDateTime.now().minusMinutes(2);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(hashOps.get(RedisKeys.STAFF_CALL_OPEN, order.getId().toString()))
                .thenReturn(firstCalledAt + "|PAYMENT_HELP|");

        StaffCallResponse response = service.call(order.getId(), customerId,
                new StaffCallRequest(StaffCallReason.ORDER_DELAY, " Been 20 minutes "));

        assertThat(response.calledAt()).isEqualTo(firstCalledAt);
        assertThat(response.reason()).isEqualTo(StaffCallReason.ORDER_DELAY);
        assertThat(response.note()).isEqualTo("Been 20 minutes");
        verify(hashOps).put(RedisKeys.STAFF_CALL_OPEN, order.getId().toString(),
                firstCalledAt + "|ORDER_DELAY|Been 20 minutes");
    }

    @Test
    void openCallsListTheReasonAndNote() {
        Order order = order(OrderStatus.PENDING);
        LocalDateTime calledAt = LocalDateTime.now().minusMinutes(1);
        when(hashOps.entries(RedisKeys.STAFF_CALL_OPEN)).thenReturn(
                Map.of(order.getId().toString(), calledAt + "|OTHER|Need a high chair | please"));
        when(orderRepository.findAllById(any())).thenReturn(List.of(order));

        List<StaffCallResponse> open = service.listOpen();

        assertThat(open).singleElement().satisfies(call -> {
            assertThat(call.reason()).isEqualTo(StaffCallReason.OTHER);
            assertThat(call.note()).isEqualTo("Need a high chair | please");
            assertThat(call.calledAt()).isEqualTo(calledAt);
        });
    }

    @Test
    void aCallCanOnlyBeAnsweredOnce() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(hashOps.delete(RedisKeys.STAFF_CALL_OPEN, order.getId().toString())).thenReturn(0L);

        assertThatThrownBy(() -> service.answer(order.getId(), UUID.randomUUID(), null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("answered");
        verify(eventPublisher, never()).publishEvent(any());
    }

    private Order order(OrderStatus status) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setFullName("Customer Luku");
        customer.setEmail("luku@example.com");

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomer(customer);
        order.setStatus(status);
        return order;
    }
}
