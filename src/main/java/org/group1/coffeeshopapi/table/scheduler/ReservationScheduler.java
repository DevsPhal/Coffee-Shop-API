package org.group1.coffeeshopapi.table.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.table.service.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

// Keeps tables in step with the clock: a table is held once its booking is close, and a booked party that
// never turns up is marked as a no-show so the table is freed. Each booking is handled in its own transaction.
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ReservationService reservationService;

    @Scheduled(initialDelay = 30_000, fixedDelay = 60_000)
    public void syncTablesWithBookings() {
        for (UUID id : reservationService.listLateForNoShow()) {
            try {
                reservationService.expireIfLate(id);
            } catch (RuntimeException e) {
                log.warn("Could not mark reservation {} as a no-show", id, e);
            }
        }
        for (UUID id : reservationService.listDueForHold()) {
            try {
                reservationService.holdTableFor(id);
            } catch (RuntimeException e) {
                log.warn("Could not hold the table for reservation {}", id, e);
            }
        }
    }
}
