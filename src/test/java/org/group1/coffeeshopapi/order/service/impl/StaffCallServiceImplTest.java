package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.TooManyRequestsException;
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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "cooldownSeconds", 30L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.<Object, Object>opsForHash()).thenReturn(hashOps);
    }

    @Test
    void theFirstCallAlertsStaffAndStartsThe30SecondCooldown() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));
        when(valueOps.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(30)))).thenReturn(true);

        StaffCallResponse response = service.call(order.getId(), customerId);

        assertThat(response.nextCallAllowedAt()).isAfter(LocalDateTime.now().plusSeconds(25));
        ArgumentCaptor<StaffCallEvent> event = ArgumentCaptor.forClass(StaffCallEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().message().type()).isEqualTo(StaffCallMessage.Type.CALLED);
        assertThat(event.getValue().message().customerName()).isEqualTo("Customer Luku");
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

        assertThatThrownBy(() -> service.call(order.getId(), customerId))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("18 seconds");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void staffCantBeCalledForAFinishedOrder() {
        Order order = order(OrderStatus.COMPLETED);
        when(orderRepository.findByIdAndCustomerId(order.getId(), customerId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.call(order.getId(), customerId))
                .isInstanceOf(InvalidOperationException.class);
        verify(valueOps, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void answeringClearsTheCallAndTellsTheCustomer() {
        Order order = order(OrderStatus.PENDING);
        UUID baristaId = UUID.randomUUID();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(hashOps.get(RedisKeys.STAFF_CALL_OPEN, order.getId().toString()))
                .thenReturn(LocalDateTime.now().minusMinutes(1).toString());
        when(hashOps.delete(RedisKeys.STAFF_CALL_OPEN, order.getId().toString())).thenReturn(1L);
        when(actorLookupService.resolve(baristaId)).thenReturn(new ActorSummary(baristaId, "Barista Dara", null));

        service.answer(order.getId(), baristaId);

        ArgumentCaptor<StaffCallEvent> event = ArgumentCaptor.forClass(StaffCallEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().message().type()).isEqualTo(StaffCallMessage.Type.ANSWERED);
        assertThat(event.getValue().message().answeredByName()).isEqualTo("Barista Dara");
        assertThat(event.getValue().customerEmail()).isEqualTo("luku@example.com");
    }

    @Test
    void aCallCanOnlyBeAnsweredOnce() {
        Order order = order(OrderStatus.PENDING);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(hashOps.delete(RedisKeys.STAFF_CALL_OPEN, order.getId().toString())).thenReturn(0L);

        assertThatThrownBy(() -> service.answer(order.getId(), UUID.randomUUID()))
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
