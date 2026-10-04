package org.group1.coffeeshopapi.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.exception.PaymentVerificationUnavailableException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BakongPaymentWatcher {

    private final OrderService orderService;
    private final BakongProperties bakongProperties;

    @Scheduled(initialDelayString = "${bakong.auto-confirm-interval-ms:10000}",
            fixedDelayString = "${bakong.auto-confirm-interval-ms:10000}")
    public void confirmPaidOrders() {
        if (!bakongProperties.isConfigured()) {
            return;
        }
        for (UUID orderId : orderService.listOrdersAwaitingBakongPayment()) {
            try {
                if (orderService.confirmBakongPaymentAutomatically(orderId)) {
                    log.info("Bakong payment confirmed automatically for order {}", orderId);
                }
            } catch (PaymentVerificationUnavailableException e) {
                log.warn("Bakong is unreachable, retrying on the next run: {}", e.getMessage());
                return;
            } catch (RuntimeException e) {
                log.warn("Could not auto-confirm Bakong payment for order {}", orderId, e);
            }
        }
        for (UUID orderId : orderService.listExpiredBakongOrders()) {
            try {
                if (orderService.cancelExpiredBakongOrder(orderId)) {
                    log.info("Cancelled order {}: its Bakong QR expired unpaid", orderId);
                }
            } catch (PaymentVerificationUnavailableException e) {
                log.warn("Bakong is unreachable, not cancelling expired orders until it answers: {}", e.getMessage());
                return;
            } catch (RuntimeException e) {
                log.warn("Could not cancel expired Bakong order {}", orderId, e);
            }
        }
    }
}
