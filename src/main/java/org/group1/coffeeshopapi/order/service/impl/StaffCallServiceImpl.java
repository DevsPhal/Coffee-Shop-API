package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.common.enums.StaffCallReason;
import org.group1.coffeeshopapi.common.enums.StaffCallStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.exception.TooManyRequestsException;
import org.group1.coffeeshopapi.order.dto.request.StaffCallRequest;
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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Value("${app.staff-call.reply-visible-minutes:60}")
    private long replyVisibleMinutes;

    @Override
    @Transactional
    public StaffCallResponse call(UUID orderId, UUID customerId, StaffCallRequest request) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getStatus().isFinished()) {
            throw new InvalidOperationException(
                    "This order is already " + order.getStatus().name().toLowerCase() + " — staff can't be called for it");
        }
        String note = StringUtils.hasText(request.note()) ? request.note().strip() : null;
        if (request.reason() == StaffCallReason.OTHER && note == null) {
            throw new InvalidOperationException("Please tell us what you need help with");
        }
        if (request.reason() == StaffCallReason.DELIVERY_HELP
                && order.getFulfillmentMethod() != FulfillmentMethod.DELIVERY) {
            throw new InvalidOperationException("Delivery help is only available for delivery orders");
        }

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
        OpenCall existing = openCall(orderId);
        OpenCall call = new OpenCall(existing != null ? existing.calledAt() : now, request.reason(), note);
        redisTemplate.opsForHash().put(RedisKeys.STAFF_CALL_OPEN, orderId.toString(), call.encode());
        redisTemplate.delete(answeredKey(orderId));

        audit(order, OrderAuditAction.STAFF_CALLED, customerId);
        publish(order, StaffCallMessage.Type.CALLED, call, null);
        return toResponse(order, call, null, now.plusSeconds(cooldownSeconds));
    }

    @Override
    public StaffCallResponse current(UUID orderId, UUID customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        LocalDateTime nextCallAllowedAt = nextCallAllowedAt(customerId);
        OpenCall open = openCall(orderId);
        if (open != null) {
            return toResponse(order, open, null, nextCallAllowedAt);
        }
        Map<Object, Object> stored = redisTemplate.opsForHash().entries(answeredKey(orderId));
        if (stored.isEmpty()) {
            return null;
        }
        Answer answer = Answer.fromHash(stored);
        return toResponse(order, answer.call(), answer, nextCallAllowedAt);
    }

    @Override
    public List<StaffCallResponse> listOpen() {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(RedisKeys.STAFF_CALL_OPEN);
        Map<UUID, OpenCall> callsById = new HashMap<>();
        entries.forEach((id, value) -> callsById.put(UUID.fromString((String) id), OpenCall.decode((String) value)));

        List<StaffCallResponse> open = new ArrayList<>();
        for (Order order : orderRepository.findAllById(callsById.keySet())) {
            if (order.getStatus().isFinished()) {
                redisTemplate.opsForHash().delete(RedisKeys.STAFF_CALL_OPEN, order.getId().toString());
                continue;
            }
            open.add(toResponse(order, callsById.get(order.getId()), null, null));
        }
        open.sort(Comparator.comparing(StaffCallResponse::calledAt));
        return open;
    }

    @Override
    @Transactional
    public StaffCallResponse answer(UUID orderId, UUID actorId, String reply) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        OpenCall call = openCall(orderId);
        Long removed = redisTemplate.opsForHash().delete(RedisKeys.STAFF_CALL_OPEN, orderId.toString());
        if (removed == null || removed == 0) {
            throw new InvalidOperationException("No open call for this order — someone may have answered it already");
        }

        LocalDateTime now = LocalDateTime.now();
        Answer answer = new Answer(
                call != null ? call : new OpenCall(now, StaffCallReason.OTHER, null),
                actorLookupService.resolve(actorId).name(),
                StringUtils.hasText(reply) ? reply.strip() : null,
                now);
        String key = answeredKey(orderId);
        redisTemplate.opsForHash().putAll(key, answer.toHash());
        redisTemplate.expire(key, Duration.ofMinutes(replyVisibleMinutes));

        audit(order, OrderAuditAction.STAFF_CALL_ANSWERED, actorId);
        publish(order, StaffCallMessage.Type.ANSWERED, answer.call(), answer);
        return toResponse(order, answer.call(), answer, null);
    }

    private String answeredKey(UUID orderId) {
        return RedisKeys.STAFF_CALL_ANSWERED_PREFIX + orderId;
    }

    private LocalDateTime nextCallAllowedAt(UUID customerId) {
        Long remaining = redisTemplate.getExpire(RedisKeys.STAFF_CALL_COOLDOWN_PREFIX + customerId);
        return remaining != null && remaining > 0 ? LocalDateTime.now().plusSeconds(remaining) : null;
    }

    private OpenCall openCall(UUID orderId) {
        Object stored = redisTemplate.opsForHash().get(RedisKeys.STAFF_CALL_OPEN, orderId.toString());
        return stored != null ? OpenCall.decode((String) stored) : null;
    }

    private void audit(Order order, OrderAuditAction action, UUID actorId) {
        OrderAuditLog log = new OrderAuditLog();
        log.setOrder(order);
        log.setAction(action);
        log.setActorId(actorId);
        orderAuditLogRepository.save(log);
    }

    private void publish(Order order, StaffCallMessage.Type type, OpenCall call, Answer answer) {
        StaffCallMessage message = new StaffCallMessage(type, order.getId(), customerName(order),
                order.getStatus(), order.getFulfillmentMethod(),
                call != null ? call.reason() : null, call != null ? call.note() : null,
                call != null ? call.calledAt() : null,
                answer != null ? answer.answeredByName() : null, answer != null ? answer.reply() : null,
                LocalDateTime.now());
        String customerEmail = order.getCustomer() != null ? order.getCustomer().getEmail() : null;
        eventPublisher.publishEvent(new StaffCallEvent(message, customerEmail));
    }

    private StaffCallResponse toResponse(Order order, OpenCall call, Answer answer, LocalDateTime nextCallAllowedAt) {
        return new StaffCallResponse(order.getId(), customerName(order), order.getStatus(),
                order.getFulfillmentMethod(), answer != null ? StaffCallStatus.ANSWERED : StaffCallStatus.OPEN,
                call.reason(), call.note(), call.calledAt(),
                answer != null ? answer.answeredByName() : null,
                answer != null ? answer.reply() : null,
                answer != null ? answer.answeredAt() : null,
                nextCallAllowedAt);
    }

    private String customerName(Order order) {
        return order.getCustomer() != null ? order.getCustomer().getFullName() : null;
    }

    record OpenCall(LocalDateTime calledAt, StaffCallReason reason, String note) {

        private static final String SEPARATOR = "|";

        String encode() {
            return calledAt + SEPARATOR + reason.name() + SEPARATOR + (note != null ? note : "");
        }

        static OpenCall decode(String value) {
            String[] parts = value.split("\\|", 3);
            LocalDateTime calledAt = LocalDateTime.parse(parts[0]);
            if (parts.length < 3) {
                return new OpenCall(calledAt, StaffCallReason.OTHER, null);
            }
            return new OpenCall(calledAt, StaffCallReason.valueOf(parts[1]), parts[2].isEmpty() ? null : parts[2]);
        }
    }

    record Answer(OpenCall call, String answeredByName, String reply, LocalDateTime answeredAt) {

        Map<String, String> toHash() {
            Map<String, String> fields = new HashMap<>();
            fields.put("calledAt", call.calledAt().toString());
            fields.put("reason", call.reason().name());
            fields.put("answeredAt", answeredAt.toString());
            if (call.note() != null) {
                fields.put("note", call.note());
            }
            if (answeredByName != null) {
                fields.put("answeredByName", answeredByName);
            }
            if (reply != null) {
                fields.put("reply", reply);
            }
            return fields;
        }

        static Answer fromHash(Map<Object, Object> fields) {
            OpenCall call = new OpenCall(LocalDateTime.parse((String) fields.get("calledAt")),
                    StaffCallReason.valueOf((String) fields.get("reason")), (String) fields.get("note"));
            return new Answer(call, (String) fields.get("answeredByName"), (String) fields.get("reply"),
                    LocalDateTime.parse((String) fields.get("answeredAt")));
        }
    }
}
