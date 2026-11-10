package org.group1.coffeeshopapi.table.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.ConflictException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.TableProperties;
import org.group1.coffeeshopapi.table.dto.request.CheckInReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.CloseReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.ReservationRequest;
import org.group1.coffeeshopapi.table.dto.response.ReservationResponse;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.group1.coffeeshopapi.table.repository.ReservationRepository;
import org.group1.coffeeshopapi.table.service.ReservationService;
import org.group1.coffeeshopapi.table.service.impl.TableLifecycle.Audit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    // A booking made for "now" is still accepted a few minutes after the time it was made for.
    private static final long PAST_GRACE_MINUTES = 5;
    private static final long UPCOMING_WINDOW_HOURS = 24;
    private static final List<ReservationStatus> ACTIVE = List.of(ReservationStatus.BOOKED, ReservationStatus.SEATED);
    private static final DateTimeFormatter END_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final ReservationRepository reservationRepository;
    private final TableLifecycle tableLifecycle;

    @Override
    public List<ReservationResponse> list(LocalDate date, ReservationStatus status, UUID tableId) {
        LocalDate day = date != null ? date : tableLifecycle.now().toLocalDate();
        return reservationRepository.findStartingBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay()).stream()
                .filter(reservation -> status == null || reservation.getStatus() == status)
                .filter(reservation -> tableId == null || (reservation.getDiningTable() != null
                        && tableId.equals(reservation.getDiningTable().getId())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ReservationResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional
    public ReservationResponse create(ReservationRequest request, UUID actorId) {
        DiningTable table = tableLifecycle.lock(request.tableId());
        LocalDateTime start = request.reservedFor().truncatedTo(ChronoUnit.MINUTES);
        requireBookableTime(start);
        requireFits(table, request.partySize());
        LocalDateTime end = start.plusMinutes(durationOf(request));
        requireNoOverlap(table, start, end, null);

        Reservation reservation = new Reservation();
        reservation.assignTable(table);
        apply(reservation, request, start, end);
        reservation.setCreatedBy(actorId);
        reservation = reservationRepository.save(reservation);

        tableLifecycle.holdIfDue(table, actorId);
        return toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse update(UUID id, ReservationRequest request, UUID actorId) {
        Reservation current = findById(id);
        UUID oldTableId = current.getDiningTable() != null ? current.getDiningTable().getId() : null;
        UUID newTableId = request.tableId();
        // Tables are always locked before bookings, in id order, so concurrent edits can't deadlock.
        DiningTable oldTable = null;
        DiningTable newTable;
        if (oldTableId == null || oldTableId.equals(newTableId)) {
            newTable = tableLifecycle.lock(newTableId);
            oldTable = oldTableId == null ? null : newTable;
        } else if (oldTableId.compareTo(newTableId) < 0) {
            oldTable = tableLifecycle.lock(oldTableId);
            newTable = tableLifecycle.lock(newTableId);
        } else {
            newTable = tableLifecycle.lock(newTableId);
            oldTable = tableLifecycle.lock(oldTableId);
        }
        Reservation reservation = lockSameTable(id, oldTableId);
        requireBooked(reservation, "changed");

        LocalDateTime start = request.reservedFor().truncatedTo(ChronoUnit.MINUTES);
        if (!start.equals(reservation.getReservedFor())) {
            requireBookableTime(start);
        }
        requireFits(newTable, request.partySize());
        LocalDateTime end = start.plusMinutes(durationOf(request));
        requireNoOverlap(newTable, start, end, reservation.getId());

        reservation.assignTable(newTable);
        apply(reservation, request, start, end);
        reservation = reservationRepository.save(reservation);

        if (oldTable != null && oldTable != newTable) {
            tableLifecycle.releaseIfUnheld(oldTable, actorId, reservation.getId());
        }
        // The booking may have moved out of (or into) the hold window.
        tableLifecycle.releaseIfUnheld(newTable, actorId, reservation.getId());
        tableLifecycle.holdIfDue(newTable, actorId);
        return toResponse(reservation);
    }

    @Override
    @Transactional
    public ReservationResponse checkIn(UUID id, CheckInReservationRequest request, UUID actorId, boolean mayOverride) {
        CheckInReservationRequest body = request != null ? request : CheckInReservationRequest.empty();
        Reservation current = findById(id);
        if (current.getDiningTable() == null) {
            throw new ConflictException("Table " + current.getTableNumber()
                    + " was deleted. Edit the booking to pick another table first.");
        }
        DiningTable table = tableLifecycle.lock(current.getDiningTable().getId());
        Reservation reservation = lockSameTable(id, table.getId());
        requireBooked(reservation, "checked in");

        LocalDateTime now = tableLifecycle.now();
        // Early arrivals are fine on the day, or within the hold window (a booking just after midnight).
        LocalDateTime holdStarts = reservation.getReservedFor().minusMinutes(settings().getHoldBeforeMinutes());
        if (reservation.getReservedFor().toLocalDate().isAfter(now.toLocalDate()) && now.isBefore(holdStarts)) {
            throw new ConflictException("This booking is for " + DAY.format(reservation.getReservedFor())
                    + ". Check the party in on the day.");
        }
        int guests = body.guestCount() != null ? body.guestCount() : reservation.getPartySize();
        requireFits(table, guests);

        String number = table.getTableNumber();
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new ConflictException("Table " + number + " is still occupied. Clear it once the guests leave, "
                    + "or move this booking to another table.");
        }
        String overrideReason = null;
        Optional<Reservation> hold = tableLifecycle.holdingReservation(table);
        if (hold.isPresent() && !hold.get().getId().equals(reservation.getId())) {
            overrideReason = tableLifecycle.requireOverride(body.overrideRequested(), mayOverride,
                    body.overrideReason(), "Table " + number + " is held for " + tableLifecycle.describe(hold.get())
                            + ". Check that party in first, or move this booking to another table.");
        }

        tableLifecycle.move(table, TableStatus.OCCUPIED, guests, TableAuditAction.RESERVATION_CHECKED_IN, actorId,
                Audit.forReservation(reservation.getId()).withOverride(overrideReason));
        reservation.setStatus(ReservationStatus.SEATED);
        reservation.setSeatedAt(now);
        return toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional
    public ReservationResponse cancel(UUID id, CloseReservationRequest request, UUID actorId) {
        return close(id, ReservationStatus.CANCELLED, reasonOf(request), actorId, false);
    }

    @Override
    @Transactional
    public ReservationResponse markNoShow(UUID id, CloseReservationRequest request, UUID actorId) {
        return close(id, ReservationStatus.NO_SHOW, reasonOf(request), actorId, false);
    }

    @Override
    public Map<UUID, List<ReservationResponse>> currentByTable(Collection<UUID> tableIds) {
        if (tableIds.isEmpty()) {
            return Map.of();
        }
        LocalDateTime before = tableLifecycle.now().plusHours(UPCOMING_WINDOW_HOURS);
        return reservationRepository.findCurrentForTables(tableIds, before, ReservationStatus.BOOKED,
                        ReservationStatus.SEATED).stream()
                .collect(Collectors.groupingBy(reservation -> reservation.getDiningTable().getId(),
                        LinkedHashMap::new, Collectors.mapping(this::toResponse, Collectors.toList())));
    }

    @Override
    public List<UUID> listDueForHold() {
        LocalDateTime dueBy = tableLifecycle.now().plusMinutes(settings().getHoldBeforeMinutes());
        return reservationRepository.findIdsBookedDueOnFreeTables(dueBy, ReservationStatus.BOOKED,
                TableStatus.AVAILABLE);
    }

    @Override
    public List<UUID> listLateForNoShow() {
        int lateAfter = settings().getNoShowAfterMinutes();
        if (lateAfter <= 0) {
            return List.of();
        }
        return reservationRepository.findIdsBookedBefore(tableLifecycle.now().minusMinutes(lateAfter),
                ReservationStatus.BOOKED);
    }

    @Override
    @Transactional
    public void holdTableFor(UUID reservationId) {
        Reservation current = reservationRepository.findById(reservationId).orElse(null);
        if (current == null || current.getStatus() != ReservationStatus.BOOKED || current.getDiningTable() == null) {
            return;
        }
        tableLifecycle.holdIfDue(tableLifecycle.lock(current.getDiningTable().getId()), null);
    }

    @Override
    @Transactional
    public void expireIfLate(UUID reservationId) {
        Reservation current = reservationRepository.findById(reservationId).orElse(null);
        int lateAfter = settings().getNoShowAfterMinutes();
        if (current == null || current.getStatus() != ReservationStatus.BOOKED || lateAfter <= 0
                || !current.getReservedFor().isBefore(tableLifecycle.now().minusMinutes(lateAfter))) {
            return;
        }
        close(reservationId, ReservationStatus.NO_SHOW,
                "Not checked in within " + lateAfter + " minutes of the booked time", null, true);
    }

    private ReservationResponse close(UUID id, ReservationStatus outcome, String reason, UUID actorId,
            boolean automatic) {
        Reservation current = findById(id);
        DiningTable table = current.getDiningTable() != null
                ? tableLifecycle.lock(current.getDiningTable().getId())
                : null;
        Reservation reservation = lockSameTable(id, table != null ? table.getId() : null);
        requireBooked(reservation, outcome == ReservationStatus.CANCELLED ? "cancelled" : "marked as a no-show");
        LocalDateTime now = tableLifecycle.now();
        if (outcome == ReservationStatus.NO_SHOW && !automatic && now.isBefore(reservation.getReservedFor())) {
            throw new ConflictException(reservation.getCustomerName() + " is not due until "
                    + tableLifecycle.formatTime(reservation.getReservedFor()) + ". Cancel the booking instead.");
        }
        reservation.setStatus(outcome);
        reservation.setClosedAt(now);
        reservation.setCloseReason(reason);
        reservation = reservationRepository.save(reservation);
        if (table != null) {
            tableLifecycle.releaseIfUnheld(table, actorId, reservation.getId());
        }
        return toResponse(reservation);
    }

    // Re-reads the booking under a row lock and makes sure it still belongs to the table locked for it.
    private Reservation lockSameTable(UUID id, UUID expectedTableId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        UUID tableId = reservation.getDiningTable() != null ? reservation.getDiningTable().getId() : null;
        if (expectedTableId == null ? tableId != null : !expectedTableId.equals(tableId)) {
            throw new ConflictException("This booking was just changed by someone else. Refresh and try again.");
        }
        return reservation;
    }

    private void requireBooked(Reservation reservation, String action) {
        switch (reservation.getStatus()) {
            case BOOKED -> { }
            case SEATED -> throw new ConflictException("This party is already seated, so the booking can't be "
                    + action + ". It completes when the table is cleared.");
            case COMPLETED -> throw new ConflictException("This booking is already completed.");
            case CANCELLED -> throw new ConflictException("This booking was cancelled.");
            case NO_SHOW -> throw new ConflictException("This booking was marked as a no-show.");
        }
    }

    private void requireBookableTime(LocalDateTime start) {
        LocalDateTime now = tableLifecycle.now();
        if (start.isBefore(now.minusMinutes(PAST_GRACE_MINUTES))) {
            throw new InvalidOperationException("Pick a time from now on");
        }
        int maxDays = settings().getMaxAdvanceDays();
        if (start.isAfter(now.plusDays(maxDays))) {
            throw new InvalidOperationException("Tables can be booked up to " + maxDays + " days ahead");
        }
    }

    private void requireFits(DiningTable table, int guests) {
        if (guests > table.getCapacity()) {
            throw new InvalidOperationException("Table " + table.getTableNumber() + " seats at most "
                    + table.getCapacity() + " guests");
        }
    }

    private void requireNoOverlap(DiningTable table, LocalDateTime start, LocalDateTime end, UUID excludeId) {
        List<Reservation> overlapping = reservationRepository.findOverlapping(table.getId(), start, end,
                excludeId != null ? excludeId : new UUID(0, 0), ACTIVE);
        if (!overlapping.isEmpty()) {
            Reservation clash = overlapping.getFirst();
            throw new ConflictException("Table " + table.getTableNumber() + " is already booked from "
                    + tableLifecycle.formatTime(clash.getReservedFor()) + " to " + END_TIME.format(clash.getEndsAt())
                    + " (" + clash.getCustomerName() + "). Pick another time or table.");
        }
    }

    private int durationOf(ReservationRequest request) {
        return request.durationMinutes() != null ? request.durationMinutes() : settings().getDefaultDurationMinutes();
    }

    private static void apply(Reservation reservation, ReservationRequest request, LocalDateTime start,
            LocalDateTime end) {
        reservation.setCustomerName(request.customerName().strip());
        reservation.setCustomerPhone(blankToNull(request.customerPhone()));
        reservation.setPartySize(request.partySize());
        reservation.setReservedFor(start);
        reservation.setEndsAt(end);
        reservation.setNote(blankToNull(request.note()));
    }

    private static String reasonOf(CloseReservationRequest request) {
        return request != null ? blankToNull(request.reason()) : null;
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }

    private TableProperties.Reservation settings() {
        return tableLifecycle.reservationSettings();
    }

    private Reservation findById(UUID id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    private ReservationResponse toResponse(Reservation reservation) {
        DiningTable table = reservation.getDiningTable();
        return new ReservationResponse(reservation.getId(), table != null ? table.getId() : null,
                reservation.currentTableNumber(), reservation.getCustomerName(), reservation.getCustomerPhone(),
                reservation.getPartySize(), reservation.getReservedFor(), reservation.getEndsAt(),
                Duration.between(reservation.getReservedFor(), reservation.getEndsAt()).toMinutes(),
                reservation.getStatus(), reservation.getNote(), reservation.getSeatedAt(), reservation.getClosedAt(),
                reservation.getCloseReason(), reservation.getCreatedAt());
    }
}
