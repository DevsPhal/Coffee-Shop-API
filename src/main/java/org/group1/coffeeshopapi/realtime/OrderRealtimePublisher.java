package org.group1.coffeeshopapi.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.realtime.dto.OrderUpdateMessage;
import org.group1.coffeeshopapi.realtime.event.OrderChangedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

// Pushes order changes only after commit, so clients never see a change that got rolled back.
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderChanged(OrderChangedEvent event) {
        OrderUpdateMessage message = new OrderUpdateMessage(event.action(), event.order(), LocalDateTime.now());
        try {
            messagingTemplate.convertAndSend(RealtimeDestinations.STAFF_ORDERS, message);
            if (event.customerEmail() != null) {
                messagingTemplate.convertAndSendToUser(event.customerEmail(), RealtimeDestinations.USER_ORDERS, message);
            }
        } catch (RuntimeException ex) {
            // The order is already saved — a failed push must not turn into an error response.
            log.warn("Failed to push order update for {}", event.order().id(), ex);
        }
    }
}
