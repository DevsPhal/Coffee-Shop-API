package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.exception.TooManyRequestsException;
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderAuditLog;
import org.group1.coffeeshopapi.order.repository.OrderAuditLogRepository;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.order.service.StaffCallService;
import org.group1.coffeeshopapi.realtime.dto.StaffCallMessage;
import org.group1.coffeeshopapi.realtime.event.StaffCallEvent;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Open calls and the cooldown live in Redis: they're short-lived and shared across app instances.
// The audit log keeps the permanent record of who called and who answered.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffCallServiceImpl implements StaffCallService {

    private final OrderRepository orderRepository;
    private final OrderAuditLogRepository orderAuditLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final ActorLookupService actorLookupService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.staff-call.cooldown-seconds:30}")
    private long cooldownSeconds;

    @Override
    @Transactional
    public StaffCallResponse call(UUID orderId, UUID customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getStatus().isFinished()) {
            throw new InvalidOperationException(
                    "This order is already " + order.getStatus().name().toLowerCase() + " — staff can't be called for it");
        }

        // SET NX is atomic, so a double-tap can't get two calls through.
        String cooldownKey = RedisKeys.STAFF_CALL_COOLDOWN_PREFIX + customerId;
        Boolean allowed = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, orderId.toString(), Duration.ofSeconds(cooldownSeconds));
        if (!Boolean.TRUE.equals(allowed)) {
            Long remaining = redisTemplate.getExpire(cooldownKey);
            long wait = remaining != null && remaining > 0 ? remaining : cooldownSeconds;
            throw new TooManyRequestsException(
                    "Staff has already been called. You can call again in " + wait + " seconds.");
        }

        LocalDateTime now = LocalDateTime.now();
        // Calling again before anyone answers keeps the original time, so staff see how long
        // the customer has really been waiting.
        redisTemplate.opsForHash().putIfAbsent(RedisKeys.STAFF_CALL_OPEN, orderId.toString(), now.toString());
        LocalDateTime calledAt = openCallTime(orderId, now);

        audit(order, OrderAuditAction.STAFF_CALLED, customerId);
        publish(order, StaffCallMessage.Type.CALLED, calledAt, null);
        return toResponse(order, calledAt, now.plusSeconds(cooldownSeconds));
    }

    @Override
    public List<StaffCallResponse> listOpen() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.STAFF_CALL_OPEN);
        Map<UUID, LocalDateTime> calledAtById = new HashMap<>();
        entries.forEach((id, calledAt) ->
                calledAtById.put(UUID.fromString((String) id), LocalDateTime.parse((String) calledAt)));

        List<StaffCallResponse> open = new ArrayList<>();
        for (Order order : orderRepository.findAllById(calledAtById.keySet())) {
            // A call on an order that has since finished is no longer anyone's job.
            if (order.getStatus().isFinished()) {
                redisTemplate.opsForHash().delete(RedisKeys.STAFF_CALL_OPEN, order.getId().toString());
                continue;
            }
            open.add(toResponse(order, calledAtById.get(order.getId()), null));
        }
        open.sort(Comparator.comparing(StaffCallResponse::calledAt));
        return open;
    }

    @Override
    @Transactional
    public void answer(UUID orderId, UUID actorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        LocalDateTime calledAt = openCallTime(orderId, null);
        // HDEL is atomic, so when two staff members tap at once only one of them answers.
        Long removed = redisTemplate.opsForHash().delete(RedisKeys.STAFF_CALL_OPEN, orderId.toString());
        if (removed == null || removed == 0) {
            throw new InvalidOperationException("No open call for this order — someone may have answered it already");
        }

        audit(order, OrderAuditAction.STAFF_CALL_ANSWERED, actorId);
        publish(order, StaffCallMessage.Type.ANSWERED, calledAt, actorLookupService.resolve(actorId).name());
    }

    private LocalDateTime openCallTime(UUID orderId, LocalDateTime fallback) {
        Object stored = redisTemplate.opsForHash().get(RedisKeys.STAFF_CALL_OPEN, orderId.toString());
        return stored != null ? LocalDateTime.parse((String) stored) : fallback;
    }

    private void audit(Order order, OrderAuditAction action, UUID actorId) {
        OrderAuditLog log = new OrderAuditLog();
        log.setOrder(order);
        log.setAction(action);
        log.setActorId(actorId);
        orderAuditLogRepository.save(log);
    }

    private void publish(Order order, StaffCallMessage.Type type, LocalDateTime calledAt, String answeredByName) {
        StaffCallMessage message = new StaffCallMessage(type, order.getId(), customerName(order),
                order.getStatus(), order.getFulfillmentMethod(), calledAt, answeredByName, LocalDateTime.now());
        String customerEmail = order.getCustomer() != null ? order.getCustomer().getEmail() : null;
        eventPublisher.publishEvent(new StaffCallEvent(message, customerEmail));
    }

    private StaffCallResponse toResponse(Order order, LocalDateTime calledAt, LocalDateTime nextCallAllowedAt) {
        return new StaffCallResponse(order.getId(), customerName(order), order.getStatus(),
                order.getFulfillmentMethod(), calledAt, nextCallAllowedAt);
    }

    private String customerName(Order order) {
        return order.getCustomer() != null ? order.getCustomer().getFullName() : null;
    }
}
