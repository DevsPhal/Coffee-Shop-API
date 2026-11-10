package org.group1.coffeeshopapi.table.service.impl;

import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.ConflictException;
import org.group1.coffeeshopapi.common.exception.ForbiddenOperationException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.TableProperties;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.table.dto.request.CheckInReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.ReservationRequest;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.group1.coffeeshopapi.table.entity.TableAuditLog;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.group1.coffeeshopapi.table.repository.ReservationRepository;
import org.group1.coffeeshopapi.table.repository.TableAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final ZoneId SHOP = ZoneId.of("Asia/Phnom_Penh");
    private static final UUID STAFF = UUID.randomUUID();

    @Mock private DiningTableRepository diningTableRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TableAuditLogRepository tableAuditLogRepository;

    private final List<TableAuditLog> auditLogs = new ArrayList<>();
    private DiningTable table;

    @BeforeEach
    void setUp() {
        table = new DiningTable();
        table.setId(UUID.randomUUID());
        table.setTableNumber("B2");
        table.setSize(TableSize.MEDIUM);
        table.setCapacity(4);
        lenient().when(diningTableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
        lenient().when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(reservationRepository.save(any(Reservation.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(tableAuditLogRepository.save(any(TableAuditLog.class))).thenAnswer(call -> {
            auditLogs.add(call.getArgument(0));
            return call.getArgument(0);
        });
    }

    @Test
    void tomorrowsBookingCantBeCheckedInToday() {
        ReservationServiceImpl service = at(LocalDateTime.of(2026, 11, 10, 19, 0));
        Reservation booking = booked("Dara", LocalDateTime.of(2026, 11, 11, 19, 0));

        assertThatThrownBy(() -> service.checkIn(booking.getId(), null, STAFF, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("11 Nov 2026");
        assertThat(booking.getStatus()).isEqualTo(ReservationStatus.BOOKED);
    }

    @Test
    void aBookingJustAfterMidnightCanBeCheckedInJustBeforeIt() {
        ReservationServiceImpl service = at(LocalDateTime.of(2026, 11, 10, 23, 40));
        Reservation booking = booked("Night Owls", LocalDateTime.of(2026, 11, 11, 0, 15));

        service.checkIn(booking.getId(), CheckInReservationRequest.empty(), STAFF, false);

        assertThat(booking.getStatus()).isEqualTo(ReservationStatus.SEATED);
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(table.getGuestCount()).isEqualTo(2);
        assertThat(auditLogs).extracting(TableAuditLog::getAction).containsExactly(TableAuditAction.RESERVATION_CHECKED_IN);
    }

    @Test
    void aPartyCantBeSeatedAtATableHeldForAnEarlierBookingWithoutAnAdminOverride() {
        ReservationServiceImpl service = at(LocalDateTime.of(2026, 11, 10, 18, 0));
        Reservation earlier = booked("Sokha", LocalDateTime.of(2026, 11, 10, 18, 15));
        Reservation later = booked("Dara", LocalDateTime.of(2026, 11, 10, 19, 45));
        table.setStatus(TableStatus.RESERVED);
        when(reservationRepository.findBookedDueBy(eq(table.getId()), any(LocalDateTime.class),
                eq(ReservationStatus.BOOKED))).thenReturn(List.of(earlier));

        assertThatThrownBy(() -> service.checkIn(later.getId(), null, STAFF, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("held for Sokha at 6:15 PM");
        assertThatThrownBy(() -> service.checkIn(later.getId(), new CheckInReservationRequest(null, true, "Sokha moved"),
                STAFF, false))
                .isInstanceOf(ForbiddenOperationException.class);

        service.checkIn(later.getId(), new CheckInReservationRequest(null, true, "Sokha moved to B3"), STAFF, true);

        assertThat(later.getStatus()).isEqualTo(ReservationStatus.SEATED);
        assertThat(auditLogs.getFirst().isOverride()).isTrue();
        assertThat(auditLogs.getFirst().getReason()).isEqualTo("Sokha moved to B3");
    }

    @Test
    void checkingInMoreGuestsThanTheTableSeatsIsRefused() {
        ReservationServiceImpl service = at(LocalDateTime.of(2026, 11, 10, 18, 0));
        Reservation booking = booked("Dara", LocalDateTime.of(2026, 11, 10, 18, 0));

        assertThatThrownBy(() -> service.checkIn(booking.getId(), new CheckInReservationRequest(5, null, null), STAFF, false))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("seats at most 4");
    }

    @Test
    void aPartyTooBigForTheTableIsRefusedBeforeAnythingIsSaved() {
        ReservationServiceImpl service = at(LocalDateTime.of(2026, 11, 10, 12, 0));

        assertThatThrownBy(() -> service.create(new ReservationRequest(table.getId(), "Big Group", null, 9,
                LocalDateTime.of(2026, 11, 10, 19, 0), null, null), STAFF))
                .isInstanceOf(InvalidOperationException.class);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void bookingsTooFarAheadAreRefused() {
        ReservationServiceImpl service = at(LocalDateTime.of(2026, 11, 10, 12, 0));

        assertThatThrownBy(() -> service.create(new ReservationRequest(table.getId(), "Planner", null, 2,
                LocalDateTime.of(2027, 3, 1, 19, 0), null, null), STAFF))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("60 days ahead");
    }

    private ReservationServiceImpl at(LocalDateTime now) {
        TableLifecycle lifecycle = new TableLifecycle(diningTableRepository, reservationRepository, orderRepository,
                tableAuditLogRepository, new TableProperties(), Clock.fixed(now.atZone(SHOP).toInstant(), SHOP));
        return new ReservationServiceImpl(reservationRepository, lifecycle);
    }

    private Reservation booked(String name, LocalDateTime at) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.assignTable(table);
        reservation.setCustomerName(name);
        reservation.setPartySize(2);
        reservation.setReservedFor(at);
        reservation.setEndsAt(at.plusMinutes(90));
        lenient().when(reservationRepository.findById(reservation.getId())).thenReturn(Optional.of(reservation));
        lenient().when(reservationRepository.findByIdForUpdate(reservation.getId())).thenReturn(Optional.of(reservation));
        return reservation;
    }
}
