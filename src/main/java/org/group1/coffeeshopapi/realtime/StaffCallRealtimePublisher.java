package org.group1.coffeeshopapi.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.realtime.dto.StaffCallMessage;
import org.group1.coffeeshopapi.realtime.event.StaffCallEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// Staff see every call and answer, so an answered alert disappears on every screen. The customer
// only hears back when someone answers.
@Slf4j
@Component
@RequiredArgsConstructor
public class StaffCallRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStaffCall(StaffCallEvent event) {
        StaffCallMessage message = event.message();
        try {
            messagingTemplate.convertAndSend(RealtimeDestinations.STAFF_CALLS, message);
            if (message.type() == StaffCallMessage.Type.ANSWERED && event.customerEmail() != null) {
                messagingTemplate.convertAndSendToUser(
                        event.customerEmail(), RealtimeDestinations.USER_STAFF_CALLS, message);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to push staff call for order {}", message.orderId(), ex);
        }
    }
}
