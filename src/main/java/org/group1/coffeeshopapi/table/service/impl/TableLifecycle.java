package org.group1.coffeeshopapi.table.service.impl;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.ConflictException;
import org.group1.coffeeshopapi.common.exception.ForbiddenOperationException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.TableProperties;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.group1.coffeeshopapi.table.entity.TableAuditLog;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.group1.coffeeshopapi.table.repository.ReservationRepository;
import org.group1.coffeeshopapi.table.repository.TableAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

// The one place a table changes status. Callers lock the table row first (lock / lockByNumber), so orders,
// check-ins, bookings and staff changes on the same table run one after another and every change is audited.
@Component
public class TableLifecycle {

    static final int MAX_REASON_LENGTH = 300;
    private static final List<OrderStatus> FINISHED_ORDER_STATUSES = Arrays.stream(OrderStatus.values())
            .filter(OrderStatus::isFinished)
            .toList();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_AND_TIME = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH);

    private final DiningTableRepository diningTableRepository;
    private final ReservationRepository reservationRepository;
    private final OrderRepository orderRepository;
    private final TableAuditLogRepository tableAuditLogRepository;
    private final TableProperties tableProperties;
    private final Clock clock;

    @Autowired
    public TableLifecycle(DiningTableRepository diningTableRepository, ReservationRepository reservationRepository,
            OrderRepository orderRepository, TableAuditLogRepository tableAuditLogRepository,
            TableProperties tableProperties) {
        this(diningTableRepository, reservationRepository, orderRepository, tableAuditLogRepository, tableProperties,
                Clock.systemDefaultZone());
    }

    TableLifecycle(DiningTableRepository diningTableRepository, ReservationRepository reservationRepository,
            OrderRepository orderRepository, TableAuditLogRepository tableAuditLogRepository,
            TableProperties tableProperties, Clock clock) {
        this.diningTableRepository = diningTableRepository;
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
        this.tableAuditLogRepository = tableAuditLogRepository;
        this.tableProperties = tableProperties;
        this.clock = clock;
    }

    // Who else is involved in a change, for the audit log.
    public record Audit(boolean override, String reason, UUID orderId, UUID reservationId) {
        public static Audit none() {
            return new Audit(false, null, null, null);
        }

        public static Audit forOrder(UUID orderId) {
            return new Audit(false, null, orderId, null);
        }

        public static Audit forReservation(UUID reservationId) {
            return new Audit(false, null, null, reservationId);
        }

        public Audit withOverride(String overrideReason) {
            return overrideReason == null ? this : new Audit(true, overrideReason, orderId, reservationId);
        }
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public TableProperties.Reservation reservationSettings() {
        return tableProperties.getReservation();
    }

    public DiningTable lock(UUID tableId) {
        return diningTableRepository.findByIdForUpdate(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
    }

    public DiningTable lockByNumber(String tableNumber) {
        return diningTableRepository.findByTableNumberForUpdate(tableNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Table " + tableNumber + " not found"));
    }

    public long activeOrderCount(UUID tableId) {
        return orderRepository.countActiveByTableId(tableId, FINISHED_ORDER_STATUSES);
    }

    // The booked party the table is being kept for: the earliest booking whose hold window has started.
    public Optional<Reservation> holdingReservation(DiningTable table) {
        LocalDateTime dueBy = now().plusMinutes(reservationSettings().getHoldBeforeMinutes());
        return reservationRepository.findBookedDueBy(table.getId(), dueBy, ReservationStatus.BOOKED).stream()
                .findFirst();
    }

    public TableAuditLog move(DiningTable table, TableStatus to, int guestCount, TableAuditAction action,
            UUID actorId, Audit audit) {
        TableAuditLog log = new TableAuditLog();
        log.setTableId(table.getId());
        log.setTableNumber(table.getTableNumber());
        log.setAction(action);
        log.setFromStatus(table.getStatus());
        log.setToStatus(to);
        log.setGuestCount(guestCount);
        log.setActorId(actorId);
        log.setOverride(audit.override());
        log.setReason(audit.reason());
        log.setOrderId(audit.orderId());
        log.setReservationId(audit.reservationId());

        table.setStatus(to);
        table.setGuestCount(guestCount);
        diningTableRepository.save(table);
        return tableAuditLogRepository.save(log);
    }

    // A free table whose booking is due is held for it.
    public void holdIfDue(DiningTable table, UUID actorId) {
        if (table.getStatus() != TableStatus.AVAILABLE) {
            return;
        }
        holdingReservation(table).ifPresent(reservation -> move(table, TableStatus.RESERVED, 0,
                TableAuditAction.RESERVATION_HELD, actorId, Audit.forReservation(reservation.getId())));
    }

    // A reserved table that no booking is holding any more is free again (or held for the next booking).
    public void releaseIfUnheld(DiningTable table, UUID actorId, UUID reservationId) {
        if (table.getStatus() != TableStatus.RESERVED) {
            return;
        }
        Optional<Reservation> next = holdingReservation(table);
        if (next.isEmpty()) {
            move(table, TableStatus.AVAILABLE, 0, TableAuditAction.RESERVATION_RELEASED, actorId,
                    Audit.forReservation(reservationId));
        }
    }

    // The seated party has left: their booking is done.
    public void completeSeatedReservations(DiningTable table) {
        LocalDateTime now = now();
        for (Reservation reservation : reservationRepository.findByTableAndStatus(table.getId(), ReservationStatus.SEATED)) {
            reservation.setStatus(ReservationStatus.COMPLETED);
            reservation.setClosedAt(now);
            reservationRepository.save(reservation);
        }
    }

    // Lets an admin past a table rule: the request must ask for it, the caller must be allowed to, and a
    // reason is required. Returns the reason to audit; throws the conflict when no override was asked for.
    public String requireOverride(boolean requested, boolean mayOverride, String reason, String conflictMessage) {
        if (!requested) {
            throw new ConflictException(conflictMessage);
        }
        if (!mayOverride) {
            throw new ForbiddenOperationException("Only an admin can override table rules. " + conflictMessage);
        }
        if (!StringUtils.hasText(reason)) {
            throw new InvalidOperationException("Give a reason for the override");
        }
        String trimmed = reason.strip();
        if (trimmed.length() > MAX_REASON_LENGTH) {
            throw new InvalidOperationException("The override reason must not exceed " + MAX_REASON_LENGTH
                    + " characters");
        }
        return trimmed;
    }

    public String describe(Reservation reservation) {
        return reservation.getCustomerName() + " at " + formatTime(reservation.getReservedFor());
    }

    public String formatTime(LocalDateTime time) {
        return time.toLocalDate().equals(now().toLocalDate()) ? TIME.format(time) : DAY_AND_TIME.format(time);
    }
}
