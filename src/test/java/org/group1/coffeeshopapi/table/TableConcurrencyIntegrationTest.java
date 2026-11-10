package org.group1.coffeeshopapi.table;

import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.ConflictException;
import org.group1.coffeeshopapi.table.dto.request.ReservationRequest;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.group1.coffeeshopapi.table.entity.TableAuditLog;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.group1.coffeeshopapi.table.repository.ReservationRepository;
import org.group1.coffeeshopapi.table.repository.TableAuditLogRepository;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.group1.coffeeshopapi.table.service.ReservationService;
import org.group1.coffeeshopapi.table.service.TableOrderClaim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// Many staff hitting the same table at the same instant, each in its own committed transaction:
// the table row lock lets exactly one of them win and the rest see the result and get a conflict.
@SpringBootTest
@ActiveProfiles("test")
class TableConcurrencyIntegrationTest {

    private static final int CALLERS = 8;

    @Autowired private DiningTableService diningTableService;
    @Autowired private ReservationService reservationService;
    @Autowired private DiningTableRepository diningTableRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TableAuditLogRepository tableAuditLogRepository;

    private final List<DiningTable> tables = new ArrayList<>();

    @AfterEach
    void removeWhatTheTestCreated() {
        for (DiningTable table : tables) {
            tableAuditLogRepository.deleteAll(tableAuditLogRepository.findByTableIdOrderByCreatedAtDesc(
                    table.getId(), Pageable.unpaged()));
            reservationRepository.deleteAll(reservationsOn(table));
            diningTableRepository.deleteById(table.getId());
        }
    }

    @Test
    void onlyOneOfManySimultaneousBookingsForTheSameSlotSucceeds() throws Exception {
        DiningTable table = table();
        LocalDateTime evening = LocalDateTime.now().plusHours(3).truncatedTo(ChronoUnit.MINUTES);

        Outcome outcome = race(caller -> () -> reservationService.create(new ReservationRequest(table.getId(),
                "Guest " + caller, null, 2, evening.plusMinutes(caller * 5L), 90, null), UUID.randomUUID()));

        assertThat(outcome.succeeded()).isEqualTo(1);
        assertThat(outcome.conflicts()).isEqualTo(CALLERS - 1);
        assertThat(reservationsOn(table)).hasSize(1)
                .allSatisfy(reservation -> assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.BOOKED));
    }

    @Test
    void onlyOneOfManySimultaneousWalkInsGetsAFreeTable() throws Exception {
        DiningTable table = table();

        Outcome outcome = race(caller -> () -> diningTableService.claimForOrder(
                TableOrderClaim.pos(table.getTableNumber(), false, false, null, UUID.randomUUID(), false)));

        assertThat(outcome.succeeded()).isEqualTo(1);
        assertThat(outcome.conflicts()).isEqualTo(CALLERS - 1);
        assertThat(diningTableRepository.findById(table.getId()).orElseThrow().getStatus())
                .isEqualTo(TableStatus.OCCUPIED);
        assertThat(tableAuditLogRepository.findByTableIdOrderByCreatedAtDesc(table.getId(), Pageable.unpaged()))
                .extracting(TableAuditLog::getAction).containsExactly(TableAuditAction.ORDER_SEATED);
    }

    @Test
    void aWalkInAndACheckInRacingForTheSameTableNeverBothSitDown() throws Exception {
        DiningTable table = table();
        LocalDateTime soon = LocalDateTime.now().plusMinutes(20).truncatedTo(ChronoUnit.MINUTES);
        UUID bookingId = reservationService.create(new ReservationRequest(table.getId(), "Booked Party", null, 2,
                soon, null, null), UUID.randomUUID()).id();
        assertThat(diningTableRepository.findById(table.getId()).orElseThrow().getStatus())
                .isEqualTo(TableStatus.RESERVED);

        Outcome outcome = race(caller -> caller == 0
                ? () -> reservationService.checkIn(bookingId, null, UUID.randomUUID(), false)
                : () -> diningTableService.claimForOrder(
                        TableOrderClaim.pos(table.getTableNumber(), false, false, null, UUID.randomUUID(), false)));

        // Only the booked party sits down; every walk-in is turned away.
        assertThat(outcome.succeeded()).isEqualTo(1);
        assertThat(outcome.conflicts()).isEqualTo(CALLERS - 1);
        assertThat(reservationService.getById(bookingId).status()).isEqualTo(ReservationStatus.SEATED);
    }

    private record Outcome(int succeeded, int conflicts) {}

    private interface Caller {
        Callable<Object> call(int caller);
    }

    // Starts every caller at the same moment and counts who won and who was refused with a conflict.
    private Outcome race(Caller callers) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CALLERS);
        CountDownLatch ready = new CountDownLatch(CALLERS);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < CALLERS; i++) {
                Callable<Object> work = callers.call(i);
                results.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        work.call();
                        return true;
                    } catch (ConflictException conflict) {
                        return false;
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            int succeeded = 0;
            int conflicts = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                } else {
                    conflicts++;
                }
            }
            return new Outcome(succeeded, conflicts);
        } finally {
            pool.shutdownNow();
        }
    }

    private List<Reservation> reservationsOn(DiningTable table) {
        return reservationRepository.findAll().stream()
                .filter(reservation -> reservation.getDiningTable() != null
                        && reservation.getDiningTable().getId().equals(table.getId()))
                .toList();
    }

    private DiningTable table() {
        DiningTable table = new DiningTable();
        table.setTableNumber("RC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        table.setSize(TableSize.MEDIUM);
        table.setCapacity(4);
        table = diningTableRepository.save(table);
        tables.add(table);
        return table;
    }
}
