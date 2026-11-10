package org.group1.coffeeshopapi.table;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.OrderType;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.ConflictException;
import org.group1.coffeeshopapi.common.exception.ForbiddenOperationException;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.table.dto.request.CheckInReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.CloseReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.ReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.dto.response.ReservationResponse;
import org.group1.coffeeshopapi.table.dto.response.TableAuditLogResponse;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.group1.coffeeshopapi.table.repository.ReservationRepository;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.group1.coffeeshopapi.table.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The whole table story against the real schema and queries: book, hold, seat, order, finish, clean.
// Rolled back after each test.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TableReservationFlowIntegrationTest {

    @Autowired private DiningTableService diningTableService;
    @Autowired private ReservationService reservationService;
    @Autowired private OrderService orderService;
    @Autowired private InventoryService inventoryService;
    @Autowired private DiningTableRepository diningTableRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private InventoryRepository inventoryRepository;

    private final UUID barista = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private Product croissant;

    @BeforeEach
    void stockACroissant() {
        Category bakery = new Category();
        bakery.setName("Bakery " + UUID.randomUUID());
        bakery.setCategoryGroup(CategoryGroup.SNACK);
        bakery = categoryRepository.save(bakery);
        croissant = new Product();
        croissant.setName("Croissant");
        croissant.setSku("IT-" + UUID.randomUUID());
        croissant.setCategory(bakery);
        croissant.setStockUnit(StockUnit.PACK);
        croissant.setSellUnit(SellUnit.PIECE);
        croissant.setUnitsPerStock(BigDecimal.ONE);
        croissant = productRepository.save(croissant);
        Inventory inventory = new Inventory();
        inventory.setProduct(croissant);
        inventoryRepository.save(inventory);
        ProductVariant piece = new ProductVariant();
        piece.setProduct(croissant);
        piece.setName(VariantLabel.PIECE);
        piece.setPrice(new BigDecimal("2.75"));
        piece.setUnitsPerSale(1);
        variantRepository.save(piece);
        inventoryService.stockIn(new StockInRequest(croissant.getId(), new BigDecimal("50"), QuantityUnit.PURCHASE,
                new BigDecimal("1.00"), null), admin);
    }

    @Test
    void aBookedPartyIsHeldSeatedServedAndTheTableCleanedInStep() {
        DiningTable table = table(4);
        LocalDateTime soon = minutesFromNow(30);

        // A booking inside the hold window holds the table straight away.
        ReservationResponse booking = reservationService.create(booking(table, "Dara", 3, soon), barista);
        assertThat(booking.status()).isEqualTo(ReservationStatus.BOOKED);
        assertThat(booking.durationMinutes()).isEqualTo(90);
        assertThat(status(table)).isEqualTo(TableStatus.RESERVED);

        // The slot is taken: an overlapping booking is refused, the next free slot is fine.
        assertThatThrownBy(() -> reservationService.create(booking(table, "Sokha", 2, soon.plusMinutes(60)), barista))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked")
                .hasMessageContaining("Dara");
        ReservationResponse later = reservationService.create(booking(table, "Sokha", 2, soon.plusMinutes(90)), barista);
        assertThat(later.status()).isEqualTo(ReservationStatus.BOOKED);

        // Walk-ins can't take the reserved table, and a barista can't override that.
        assertThatThrownBy(() -> orderService.create(walkIn(table), barista))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reserved for Dara");
        assertThatThrownBy(() -> orderService.create(overriding(table, "Party of two"), barista, false))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThat(status(table)).isEqualTo(TableStatus.RESERVED);

        // Dara arrives: check in, the table is occupied and the booking seated.
        ReservationResponse seated = reservationService.checkIn(booking.id(), CheckInReservationRequest.empty(),
                barista, false);
        assertThat(seated.status()).isEqualTo(ReservationStatus.SEATED);
        assertThat(status(table)).isEqualTo(TableStatus.OCCUPIED);
        assertThat(diningTableRepository.findById(table.getId()).orElseThrow().getGuestCount()).isEqualTo(3);
        assertThatThrownBy(() -> reservationService.checkIn(booking.id(), null, barista, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already seated");

        // A walk-in still can't share the table; the seated party's orders can, explicitly.
        assertThatThrownBy(() -> orderService.create(walkIn(table), barista))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("occupied");
        OrderResponse first = orderService.create(forSeatedParty(table), barista);
        OrderResponse second = orderService.create(forSeatedParty(table), barista);
        assertThat(first.tableNumber()).isEqualTo(table.getTableNumber());
        assertThat(activeOrders(table)).isEqualTo(2);

        // The table frees itself only when the last order is done; then it needs cleaning.
        orderService.cancelAny(first.id(), admin);
        assertThat(status(table)).isEqualTo(TableStatus.OCCUPIED);
        orderService.cancelAny(second.id(), admin);
        assertThat(orderRepository.findById(second.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(status(table)).isEqualTo(TableStatus.CLEANING);
        assertThat(reservationService.getById(booking.id()).status()).isEqualTo(ReservationStatus.COMPLETED);
        assertThatThrownBy(() -> orderService.create(walkIn(table), barista))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("needs cleaning");

        // Sokha's booking is 2 hours out, outside the hold window, so a clean table is free again.
        diningTableService.updateStatus(table.getId(), new UpdateTableStatusRequest(TableStatus.AVAILABLE, null),
                barista, false);
        assertThat(status(table)).isEqualTo(TableStatus.AVAILABLE);

        // Moving Sokha forward brings the booking into the hold window: the table is held again.
        reservationService.update(later.id(), booking(table, "Sokha", 2, minutesFromNow(40)), barista);
        assertThat(status(table)).isEqualTo(TableStatus.RESERVED);

        // An admin gives the held table to a walk-in, with a reason, and it is on the record.
        OrderResponse walkIn = orderService.create(overriding(table, "Walk-in couple, leaving before 7"), admin, true);
        assertThat(status(table)).isEqualTo(TableStatus.OCCUPIED);

        // Sokha can't be seated at an occupied table, even by an admin.
        assertThatThrownBy(() -> reservationService.checkIn(later.id(), null, admin, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("still occupied");

        // The walk-in leaves; once cleaned, the table goes straight back to being held for Sokha.
        orderService.cancelAny(walkIn.id(), admin);
        diningTableService.updateStatus(table.getId(), new UpdateTableStatusRequest(TableStatus.AVAILABLE, null),
                barista, false);
        assertThat(status(table)).isEqualTo(TableStatus.RESERVED);
        reservationService.checkIn(later.id(), new CheckInReservationRequest(2, null, null), barista, false);
        assertThat(status(table)).isEqualTo(TableStatus.OCCUPIED);

        List<TableAuditLogResponse> history = diningTableService.history(table.getId());
        assertThat(history).extracting(TableAuditLogResponse::action).contains(
                TableAuditAction.RESERVATION_HELD, TableAuditAction.RESERVATION_CHECKED_IN,
                TableAuditAction.ORDER_ADDED, TableAuditAction.ORDERS_FINISHED, TableAuditAction.STATUS_CHANGED,
                TableAuditAction.ORDER_SEATED);
        TableAuditLogResponse override = history.stream().filter(TableAuditLogResponse::override).findFirst()
                .orElseThrow();
        assertThat(override.action()).isEqualTo(TableAuditAction.ORDER_SEATED);
        assertThat(override.reason()).isEqualTo("Walk-in couple, leaving before 7");
        assertThat(override.orderId()).isEqualTo(walkIn.id());
        assertThat(override.fromStatus()).isEqualTo(TableStatus.RESERVED);
        assertThat(history).allSatisfy(entry -> assertThat(entry.createdAt()).isNotNull());
    }

    @Test
    void cancelledAndMissedBookingsFreeTheirTable() {
        DiningTable cancelled = table(4);
        ReservationResponse booking = reservationService.create(booking(cancelled, "Dara", 2, minutesFromNow(20)), barista);
        assertThat(status(cancelled)).isEqualTo(TableStatus.RESERVED);
        assertThatThrownBy(() -> reservationService.markNoShow(booking.id(), null, barista))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cancel the booking instead");

        reservationService.cancel(booking.id(), new CloseReservationRequest("Called to cancel"), barista);
        assertThat(status(cancelled)).isEqualTo(TableStatus.AVAILABLE);
        assertThat(reservationService.getById(booking.id()).closeReason()).isEqualTo("Called to cancel");

        // A party 40 minutes late is marked as a no-show by the scheduler, and the table is freed.
        DiningTable missed = table(2);
        Reservation late = new Reservation();
        late.assignTable(missed);
        late.setCustomerName("Late Party");
        late.setPartySize(2);
        late.setReservedFor(minutesFromNow(-40));
        late.setEndsAt(minutesFromNow(50));
        late = reservationRepository.save(late);
        missed.setStatus(TableStatus.RESERVED);
        diningTableRepository.save(missed);
        assertThat(reservationService.listLateForNoShow()).contains(late.getId());

        reservationService.expireIfLate(late.getId());

        assertThat(reservationService.getById(late.getId()).status()).isEqualTo(ReservationStatus.NO_SHOW);
        assertThat(status(missed)).isEqualTo(TableStatus.AVAILABLE);
        assertThat(diningTableService.history(missed.getId()).getFirst().actor()).isNull();
    }

    @Test
    void bookingsMustFitTheTableAndItsDiary() {
        DiningTable small = table(2);

        assertThatThrownBy(() -> reservationService.create(booking(small, "Big Group", 6, minutesFromNow(180)), barista))
                .hasMessageContaining("seats at most 2");
        assertThatThrownBy(() -> reservationService.create(booking(small, "Yesterday", 2, minutesFromNow(-60)), barista))
                .hasMessageContaining("from now on");

        ReservationResponse tonight = reservationService.create(booking(small, "Vanna", 2, minutesFromNow(240)), barista);
        assertThat(status(small)).isEqualTo(TableStatus.AVAILABLE);
        assertThat(reservationService.list(tonight.reservedFor().toLocalDate(), null, small.getId()))
                .extracting(ReservationResponse::id).containsExactly(tonight.id());

        // The table can't shrink below a booking on it, or be deleted while it has one.
        assertThatThrownBy(() -> diningTableService.delete(small.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("1 upcoming booking");
    }

    private ReservationRequest booking(DiningTable table, String name, int partySize, LocalDateTime at) {
        return new ReservationRequest(table.getId(), name, "012 345 678", partySize, at, null, null);
    }

    private StaffCreateOrderRequest walkIn(DiningTable table) {
        return new StaffCreateOrderRequest(items(), null, OrderType.DINE_IN, table.getTableNumber());
    }

    private StaffCreateOrderRequest forSeatedParty(DiningTable table) {
        return new StaffCreateOrderRequest(items(), null, OrderType.DINE_IN, table.getTableNumber(), true, null, null);
    }

    private StaffCreateOrderRequest overriding(DiningTable table, String reason) {
        return new StaffCreateOrderRequest(items(), null, OrderType.DINE_IN, table.getTableNumber(), null, true, reason);
    }

    private List<OrderItemRequest> items() {
        return List.of(new OrderItemRequest(croissant.getId(), 1, null, null, null, null, null, null));
    }

    private DiningTable table(int capacity) {
        DiningTable table = new DiningTable();
        table.setTableNumber("IT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        table.setSize(TableSize.MEDIUM);
        table.setCapacity(capacity);
        return diningTableRepository.save(table);
    }

    private TableStatus status(DiningTable table) {
        return diningTableRepository.findById(table.getId()).orElseThrow().getStatus();
    }

    private long activeOrders(DiningTable table) {
        return orderRepository.countActiveByTableId(table.getId(),
                List.of(OrderStatus.COMPLETED, OrderStatus.DELIVERED, OrderStatus.CANCELLED));
    }

    private static LocalDateTime minutesFromNow(long minutes) {
        return LocalDateTime.now().plusMinutes(minutes).truncatedTo(ChronoUnit.MINUTES);
    }
}
