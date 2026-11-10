package org.group1.coffeeshopapi.table.service.impl;

import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.ConflictException;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ForbiddenOperationException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.TableProperties;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.table.dto.request.CreateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.group1.coffeeshopapi.table.entity.TableAuditLog;
import org.group1.coffeeshopapi.table.mapper.DiningTableMapper;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.group1.coffeeshopapi.table.repository.ReservationRepository;
import org.group1.coffeeshopapi.table.repository.TableAuditLogRepository;
import org.group1.coffeeshopapi.table.service.TableOrderClaim;
import org.group1.coffeeshopapi.table.service.TableSeating;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiningTableServiceImplTest {

    private static final ZoneId SHOP = ZoneId.of("Asia/Phnom_Penh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 18, 0);
    private static final UUID STAFF = UUID.randomUUID();

    @Mock private DiningTableRepository diningTableRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private TableAuditLogRepository tableAuditLogRepository;
    @Mock private DiningTableMapper diningTableMapper;
    @Mock private ActorLookupService actorLookupService;

    private final TableProperties tableProperties = new TableProperties();
    private final List<TableAuditLog> auditLogs = new ArrayList<>();
    private DiningTableServiceImpl service;

    @BeforeEach
    void setUp() {
        TableLifecycle lifecycle = new TableLifecycle(diningTableRepository, reservationRepository, orderRepository,
                tableAuditLogRepository, tableProperties, Clock.fixed(NOW.atZone(SHOP).toInstant(), SHOP));
        service = new DiningTableServiceImpl(diningTableRepository, orderRepository, reservationRepository,
                tableAuditLogRepository, diningTableMapper, tableProperties, lifecycle, actorLookupService);
        lenient().when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(tableAuditLogRepository.save(any(TableAuditLog.class))).thenAnswer(call -> {
            auditLogs.add(call.getArgument(0));
            return call.getArgument(0);
        });
    }

    @Test
    void creatingATableNormalizesTheNumberAndDefaultsCapacityFromSize() {
        service.create(new CreateTableRequest(" a1 ", TableSize.LARGE, null));

        ArgumentCaptor<DiningTable> captor = ArgumentCaptor.forClass(DiningTable.class);
        verify(diningTableRepository).save(captor.capture());
        assertThat(captor.getValue().getTableNumber()).isEqualTo("A1");
        assertThat(captor.getValue().getCapacity()).isEqualTo(6);
        verify(diningTableMapper).toResponse(captor.getValue(), "https://590stcafe.shop/table/A1");
    }

    @Test
    void creatingADuplicateTableNumberIsRejected() {
        when(diningTableRepository.existsByTableNumberIgnoreCase("5")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateTableRequest("5", TableSize.SMALL, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ---- Manual status changes ----

    @Test
    void occupyingATableRequiresAtLeastOneGuest() {
        DiningTable table = locked(TableStatus.AVAILABLE, 4, 0);

        assertThatThrownBy(() -> barista(table, new UpdateTableStatusRequest(TableStatus.OCCUPIED, 0)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void guestsCannotExceedCapacity() {
        DiningTable table = locked(TableStatus.AVAILABLE, 2, 0);

        assertThatThrownBy(() -> barista(table, new UpdateTableStatusRequest(TableStatus.OCCUPIED, 3)))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void seatingWalkInGuestsAtAFreeTableIsAuditedWithWhoDidIt() {
        DiningTable table = locked(TableStatus.AVAILABLE, 4, 0);

        barista(table, new UpdateTableStatusRequest(TableStatus.OCCUPIED, 3));

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(table.getGuestCount()).isEqualTo(3);
        TableAuditLog log = onlyLog();
        assertThat(log.getAction()).isEqualTo(TableAuditAction.STATUS_CHANGED);
        assertThat(log.getFromStatus()).isEqualTo(TableStatus.AVAILABLE);
        assertThat(log.getToStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(log.getActorId()).isEqualTo(STAFF);
        assertThat(log.isOverride()).isFalse();
    }

    @Test
    void whenGuestsLeaveTheTableNeedsCleaningAndTheirBookingCompletes() {
        DiningTable table = locked(TableStatus.OCCUPIED, 4, 3);
        Reservation seated = reservation(table, ReservationStatus.SEATED, NOW.minusHours(1));
        when(reservationRepository.findByTableAndStatus(table.getId(), ReservationStatus.SEATED)).thenReturn(List.of(seated));

        barista(table, new UpdateTableStatusRequest(TableStatus.CLEANING, null));

        assertThat(table.getStatus()).isEqualTo(TableStatus.CLEANING);
        assertThat(table.getGuestCount()).isZero();
        assertThat(seated.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(seated.getClosedAt()).isEqualTo(NOW);
    }

    @Test
    void aPartyWithActiveOrdersCannotBeClearedEvenByAnAdminOverride() {
        DiningTable table = locked(TableStatus.OCCUPIED, 4, 2);
        when(orderRepository.countActiveByTableId(eq(table.getId()), anyList())).thenReturn(2L);

        assertThatThrownBy(() -> barista(table, new UpdateTableStatusRequest(TableStatus.CLEANING, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("2 active orders");
        assertThatThrownBy(() -> admin(table, overriding(TableStatus.AVAILABLE, "Guests walked out")))
                .isInstanceOf(ConflictException.class);
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(auditLogs).isEmpty();
    }

    @Test
    void skippingCleaningNeedsAnAdminOverrideWithAReason() {
        DiningTable table = locked(TableStatus.OCCUPIED, 4, 2);

        assertThatThrownBy(() -> barista(table, new UpdateTableStatusRequest(TableStatus.AVAILABLE, null)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> barista(table, overriding(TableStatus.AVAILABLE, "Already wiped")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> admin(table, overriding(TableStatus.AVAILABLE, "  ")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("reason");
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);

        admin(table, overriding(TableStatus.AVAILABLE, " Already wiped "));

        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
        TableAuditLog log = onlyLog();
        assertThat(log.isOverride()).isTrue();
        assertThat(log.getReason()).isEqualTo("Already wiped");
    }

    @Test
    void aTableIsNeverReservedByHand() {
        DiningTable table = locked(TableStatus.AVAILABLE, 4, 0);

        assertThatThrownBy(() -> admin(table, overriding(TableStatus.RESERVED, "VIP")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reservation");
    }

    @Test
    void aReservedTableIsOnlyFreedByCancellingItsBooking() {
        DiningTable table = locked(TableStatus.RESERVED, 4, 0);
        Reservation booked = reservation(table, ReservationStatus.BOOKED, NOW.plusMinutes(30));
        bookingsDue(table, booked);

        assertThatThrownBy(() -> barista(table, new UpdateTableStatusRequest(TableStatus.AVAILABLE, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Dara at 6:30 PM");
    }

    @Test
    void aStaleReservedTableWithNoBookingCanBeFreed() {
        DiningTable table = locked(TableStatus.RESERVED, 4, 0);

        barista(table, new UpdateTableStatusRequest(TableStatus.AVAILABLE, null));

        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    void seatingAWalkInAtAReservedTableNeedsAnAdminOverride() {
        DiningTable table = locked(TableStatus.RESERVED, 4, 0);
        bookingsDue(table, reservation(table, ReservationStatus.BOOKED, NOW.plusMinutes(30)));

        assertThatThrownBy(() -> barista(table, new UpdateTableStatusRequest(TableStatus.OCCUPIED, 2)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("held for Dara");

        admin(table, new UpdateTableStatusRequest(TableStatus.OCCUPIED, 2, true, "Party of 2 is late, table back by 6:30"));

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(onlyLog().isOverride()).isTrue();
    }

    @Test
    void markingATableCleanHoldsItForABookingThatIsDue() {
        DiningTable table = locked(TableStatus.CLEANING, 4, 0);
        Reservation booked = reservation(table, ReservationStatus.BOOKED, NOW.plusMinutes(45));
        bookingsDue(table, booked);

        barista(table, new UpdateTableStatusRequest(TableStatus.AVAILABLE, null));

        assertThat(table.getStatus()).isEqualTo(TableStatus.RESERVED);
        assertThat(auditLogs).extracting(TableAuditLog::getAction)
                .containsExactly(TableAuditAction.STATUS_CHANGED, TableAuditAction.RESERVATION_HELD);
        assertThat(auditLogs.get(1).getReservationId()).isEqualTo(booked.getId());
    }

    // ---- Orders claiming a table ----

    @Test
    void aWalkInOrderSeatsAFreeTable() {
        DiningTable table = lockedByNumber(TableStatus.AVAILABLE);

        TableSeating seating = service.claimForOrder(pos(false));

        assertThat(seating.table()).isSameAs(table);
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(table.getGuestCount()).isEqualTo(1);
        assertThat(seating.auditLog().getAction()).isEqualTo(TableAuditAction.ORDER_SEATED);

        UUID orderId = UUID.randomUUID();
        service.linkOrder(seating, orderId);
        assertThat(seating.auditLog().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void aWalkInOrderIsRefusedAtAnOccupiedTable() {
        DiningTable table = lockedByNumber(TableStatus.OCCUPIED);

        assertThatThrownBy(() -> service.claimForOrder(pos(false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("occupied");
        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(auditLogs).isEmpty();
    }

    @Test
    void anotherRoundForTheSeatedPartyIsAddedToTheOccupiedTable() {
        DiningTable table = lockedByNumber(TableStatus.OCCUPIED);
        table.setGuestCount(3);

        TableSeating seating = service.claimForOrder(pos(true));

        assertThat(table.getGuestCount()).isEqualTo(3);
        assertThat(seating.auditLog().getAction()).isEqualTo(TableAuditAction.ORDER_ADDED);
    }

    @Test
    void aCustomerAtAnOccupiedTableAddsToTheirParty() {
        lockedByNumber(TableStatus.OCCUPIED);

        TableSeating seating = service.claimForOrder(TableOrderClaim.customer("a1", UUID.randomUUID()));

        assertThat(seating.auditLog().getAction()).isEqualTo(TableAuditAction.ORDER_ADDED);
    }

    @Test
    void reservedTablesRefuseWalkInsAndCustomers() {
        DiningTable table = lockedByNumber(TableStatus.RESERVED);
        bookingsDue(table, reservation(table, ReservationStatus.BOOKED, NOW.plusMinutes(20)));

        assertThatThrownBy(() -> service.claimForOrder(pos(false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reserved for Dara at 6:20 PM");
        assertThatThrownBy(() -> service.claimForOrder(pos(true)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.claimForOrder(TableOrderClaim.customer("A1", UUID.randomUUID())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ask our staff");
        assertThat(table.getStatus()).isEqualTo(TableStatus.RESERVED);
    }

    @Test
    void aFreeTableWhoseBookingIsDueIsTreatedAsReserved() {
        DiningTable table = lockedByNumber(TableStatus.AVAILABLE);
        bookingsDue(table, reservation(table, ReservationStatus.BOOKED, NOW.plusMinutes(10)));

        assertThatThrownBy(() -> service.claimForOrder(pos(false))).isInstanceOf(ConflictException.class);
        assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
    }

    @Test
    void onlyAnAdminCanOverrideAReservedTableForAnOrder() {
        DiningTable table = lockedByNumber(TableStatus.RESERVED);

        assertThatThrownBy(() -> service.claimForOrder(TableOrderClaim.pos("A1", false, true, "Late party", STAFF, false)))
                .isInstanceOf(ForbiddenOperationException.class);

        TableSeating seating = service.claimForOrder(TableOrderClaim.pos("A1", false, true, "Late party", STAFF, true));

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(seating.auditLog().isOverride()).isTrue();
        assertThat(seating.auditLog().getReason()).isEqualTo("Late party");
    }

    @Test
    void aTableWaitingForCleaningRefusesWalkInsButTheSeatedPartyCanReopenIt() {
        DiningTable table = lockedByNumber(TableStatus.CLEANING);

        assertThatThrownBy(() -> service.claimForOrder(pos(false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("needs cleaning");

        service.claimForOrder(TableOrderClaim.customer("A1", UUID.randomUUID()));

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void dineInWithoutATableNumberIsRejected() {
        assertThatThrownBy(() -> service.claimForOrder(TableOrderClaim.customer(" ", UUID.randomUUID())))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("table number");
    }

    // ---- Orders finishing ----

    @Test
    void whenTheLastOrderFinishesTheTableNeedsCleaning() {
        DiningTable table = locked(TableStatus.OCCUPIED, 4, 2);
        UUID orderId = UUID.randomUUID();

        service.releaseAfterOrder(table.getId(), orderId, STAFF);

        assertThat(table.getStatus()).isEqualTo(TableStatus.CLEANING);
        assertThat(table.getGuestCount()).isZero();
        TableAuditLog log = onlyLog();
        assertThat(log.getAction()).isEqualTo(TableAuditAction.ORDERS_FINISHED);
        assertThat(log.getOrderId()).isEqualTo(orderId);
    }

    @Test
    void theTableStaysOccupiedWhileOtherOrdersAreStillActive() {
        DiningTable table = locked(TableStatus.OCCUPIED, 4, 2);
        when(orderRepository.countActiveByTableId(eq(table.getId()), anyList())).thenReturn(1L);

        service.releaseAfterOrder(table.getId(), UUID.randomUUID(), STAFF);

        assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
        assertThat(auditLogs).isEmpty();
    }

    @Test
    void anOrderFinishingAtATableThatIsNoLongerOccupiedChangesNothing() {
        DiningTable table = locked(TableStatus.CLEANING, 4, 0);

        service.releaseAfterOrder(table.getId(), UUID.randomUUID(), STAFF);

        assertThat(table.getStatus()).isEqualTo(TableStatus.CLEANING);
        assertThat(auditLogs).isEmpty();
    }

    // ---- Deleting ----

    @Test
    void anOccupiedTableCannotBeDeleted() {
        DiningTable table = locked(TableStatus.OCCUPIED, 4, 2);

        assertThatThrownBy(() -> service.delete(table.getId())).isInstanceOf(InvalidOperationException.class);
        verify(orderRepository, never()).detachDiningTable(eq(table.getId()));
    }

    @Test
    void aTableWithUpcomingBookingsCannotBeDeleted() {
        DiningTable table = locked(TableStatus.AVAILABLE, 4, 0);
        when(reservationRepository.countByDiningTableIdAndStatusIn(eq(table.getId()), anyList())).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(table.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("1 upcoming booking");
        verify(diningTableRepository, never()).delete(table);
    }

    private void barista(DiningTable table, UpdateTableStatusRequest request) {
        service.updateStatus(table.getId(), request, STAFF, false);
    }

    private void admin(DiningTable table, UpdateTableStatusRequest request) {
        service.updateStatus(table.getId(), request, STAFF, true);
    }

    private static UpdateTableStatusRequest overriding(TableStatus status, String reason) {
        return new UpdateTableStatusRequest(status, null, true, reason);
    }

    private static TableOrderClaim pos(boolean forSeatedParty) {
        return TableOrderClaim.pos("a1", forSeatedParty, false, null, STAFF, false);
    }

    private TableAuditLog onlyLog() {
        assertThat(auditLogs).hasSize(1);
        return auditLogs.getFirst();
    }

    private void bookingsDue(DiningTable table, Reservation... reservations) {
        lenient().when(reservationRepository.findBookedDueBy(eq(table.getId()), any(LocalDateTime.class),
                eq(ReservationStatus.BOOKED))).thenReturn(List.of(reservations));
    }

    private DiningTable locked(TableStatus status, int capacity, int guests) {
        DiningTable table = table(status, capacity, guests);
        when(diningTableRepository.findByIdForUpdate(table.getId())).thenReturn(Optional.of(table));
        return table;
    }

    private DiningTable lockedByNumber(TableStatus status) {
        DiningTable table = table(status, 4, status == TableStatus.OCCUPIED ? 2 : 0);
        when(diningTableRepository.findByTableNumberForUpdate("A1")).thenReturn(Optional.of(table));
        return table;
    }

    private static Reservation reservation(DiningTable table, ReservationStatus status, LocalDateTime at) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.assignTable(table);
        reservation.setCustomerName("Dara");
        reservation.setPartySize(2);
        reservation.setReservedFor(at);
        reservation.setEndsAt(at.plusMinutes(90));
        reservation.setStatus(status);
        return reservation;
    }

    private static DiningTable table(TableStatus status, int capacity, int guests) {
        DiningTable table = new DiningTable();
        table.setId(UUID.randomUUID());
        table.setTableNumber("A1");
        table.setSize(TableSize.MEDIUM);
        table.setCapacity(capacity);
        table.setGuestCount(guests);
        table.setStatus(status);
        return table;
    }
}
