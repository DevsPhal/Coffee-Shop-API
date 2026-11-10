package org.group1.coffeeshopapi.table.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.common.exception.ConflictException;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.properties.TableProperties;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.table.dto.request.CreateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.dto.response.TableAuditLogResponse;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.group1.coffeeshopapi.table.entity.Reservation;
import org.group1.coffeeshopapi.table.entity.TableAuditLog;
import org.group1.coffeeshopapi.table.mapper.DiningTableMapper;
import org.group1.coffeeshopapi.table.repository.DiningTableRepository;
import org.group1.coffeeshopapi.table.repository.ReservationRepository;
import org.group1.coffeeshopapi.table.repository.TableAuditLogRepository;
import org.group1.coffeeshopapi.table.service.DiningTableService;
import org.group1.coffeeshopapi.table.service.TableOrderClaim;
import org.group1.coffeeshopapi.table.service.TableRules;
import org.group1.coffeeshopapi.table.service.TableSeating;
import org.group1.coffeeshopapi.table.service.impl.TableLifecycle.Audit;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiningTableServiceImpl implements DiningTableService {

    private static final int HISTORY_LIMIT = 200;
    private static final List<ReservationStatus> ACTIVE_RESERVATIONS = List.of(
            ReservationStatus.BOOKED, ReservationStatus.SEATED);

    private final DiningTableRepository diningTableRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final TableAuditLogRepository tableAuditLogRepository;
    private final DiningTableMapper diningTableMapper;
    private final TableProperties tableProperties;
    private final TableLifecycle tableLifecycle;
    private final ActorLookupService actorLookupService;

    @Override
    @Transactional
    public TableResponse create(CreateTableRequest request) {
        String tableNumber = normalize(request.tableNumber());
        if (diningTableRepository.existsByTableNumberIgnoreCase(tableNumber)) {
            throw new DuplicateResourceException("Table " + tableNumber + " already exists");
        }
        DiningTable table = new DiningTable();
        table.setTableNumber(tableNumber);
        table.setSize(request.size());
        table.setCapacity(request.capacity() != null ? request.capacity() : request.size().defaultCapacity());
        return toResponse(diningTableRepository.save(table));
    }

    @Override
    public TableResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<TableResponse> list(TableStatus status, Pageable pageable) {
        Page<DiningTable> tables = status == null
                ? diningTableRepository.findAllByOrderByTableNumberAsc(pageable)
                : diningTableRepository.findByStatusOrderByTableNumberAsc(status, pageable);
        return tables.map(this::toResponse);
    }

    @Override
    @Transactional
    public TableResponse update(UUID id, UpdateTableRequest request) {
        DiningTable table = tableLifecycle.lock(id);
        if (StringUtils.hasText(request.tableNumber())) {
            String tableNumber = normalize(request.tableNumber());
            if (diningTableRepository.existsByTableNumberIgnoreCaseAndIdNot(tableNumber, id)) {
                throw new DuplicateResourceException("Table " + tableNumber + " already exists");
            }
            table.setTableNumber(tableNumber);
        }
        if (request.size() != null) {
            table.setSize(request.size());
            if (request.capacity() == null) {
                table.setCapacity(request.size().defaultCapacity());
            }
        }
        if (request.capacity() != null) {
            table.setCapacity(request.capacity());
        }
        if (table.getGuestCount() > table.getCapacity()) {
            throw new InvalidOperationException("Table " + table.getTableNumber() + " currently has "
                    + table.getGuestCount() + " guests — capacity can't be lower than that");
        }
        int largestBooking = reservationRepository.findLargestBookedParty(id, ReservationStatus.BOOKED);
        if (largestBooking > table.getCapacity()) {
            throw new InvalidOperationException("Table " + table.getTableNumber() + " has a booking for "
                    + largestBooking + " guests — capacity can't be lower than that");
        }
        return toResponse(diningTableRepository.save(table));
    }

    @Override
    @Transactional
    public TableResponse updateStatus(UUID id, UpdateTableStatusRequest request, UUID actorId, boolean mayOverride) {
        DiningTable table = tableLifecycle.lock(id);
        String number = table.getTableNumber();
        TableStatus from = table.getStatus();
        TableStatus to = request.status();
        int guestCount = to == TableStatus.OCCUPIED && request.guestCount() != null ? request.guestCount() : 0;
        if (to == TableStatus.OCCUPIED && guestCount < 1) {
            throw new InvalidOperationException("An occupied table needs at least 1 guest");
        }
        if (guestCount > table.getCapacity()) {
            throw new InvalidOperationException("Table " + number + " seats at most " + table.getCapacity() + " guests");
        }
        if (from == to) {
            if (to == TableStatus.OCCUPIED && guestCount != table.getGuestCount()) {
                tableLifecycle.move(table, to, guestCount, TableAuditAction.STATUS_CHANGED, actorId, Audit.none());
            }
            return toResponse(table);
        }

        TableRules.Decision decision = TableRules.manualChange(from, to, number);
        if (decision.verdict() == TableRules.Verdict.NEVER) {
            throw new ConflictException(decision.message());
        }
        if (from == TableStatus.OCCUPIED) {
            long activeOrders = tableLifecycle.activeOrderCount(id);
            if (activeOrders > 0) {
                throw new ConflictException("Table " + number + " still has " + activeOrders + " active "
                        + (activeOrders == 1 ? "order" : "orders") + ". Complete or cancel "
                        + (activeOrders == 1 ? "it" : "them") + " before clearing the table.");
            }
        }
        Optional<Reservation> hold = from == TableStatus.OCCUPIED
                ? Optional.empty()
                : tableLifecycle.holdingReservation(table);
        if (from == TableStatus.RESERVED && to == TableStatus.AVAILABLE && hold.isPresent()) {
            throw new ConflictException("Table " + number + " is held for " + tableLifecycle.describe(hold.get())
                    + ". Cancel the booking or mark it as a no-show to free the table.");
        }
        String overrideMessage = decision.verdict() == TableRules.Verdict.ADMIN_OVERRIDE ? decision.message() : null;
        if (to == TableStatus.OCCUPIED && hold.isPresent()) {
            overrideMessage = "Table " + number + " is held for " + tableLifecycle.describe(hold.get())
                    + ". Check that booking in, or seat these guests at another table.";
        }
        String overrideReason = overrideMessage == null ? null : tableLifecycle.requireOverride(
                request.overrideRequested(), mayOverride, request.overrideReason(), overrideMessage);

        if (from == TableStatus.OCCUPIED) {
            tableLifecycle.completeSeatedReservations(table);
        }
        tableLifecycle.move(table, to, guestCount, TableAuditAction.STATUS_CHANGED, actorId,
                Audit.none().withOverride(overrideReason));
        if (to == TableStatus.AVAILABLE) {
            tableLifecycle.holdIfDue(table, actorId);
        }
        return toResponse(table);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DiningTable table = tableLifecycle.lock(id);
        if (table.getStatus() == TableStatus.OCCUPIED) {
            throw new InvalidOperationException("Table " + table.getTableNumber()
                    + " is occupied — clear it before deleting");
        }
        long bookings = reservationRepository.countByDiningTableIdAndStatusIn(id, ACTIVE_RESERVATIONS);
        if (bookings > 0) {
            throw new ConflictException("Table " + table.getTableNumber() + " has " + bookings + " upcoming "
                    + (bookings == 1 ? "booking" : "bookings") + ". Move or cancel "
                    + (bookings == 1 ? "it" : "them") + " before deleting the table.");
        }
        orderRepository.detachDiningTable(id);
        reservationRepository.detachDiningTable(id);
        diningTableRepository.delete(table);
    }

    @Override
    public TableResponse getByNumber(String tableNumber) {
        return toResponse(requireByNumber(tableNumber));
    }

    @Override
    public List<TableResponse> listPublic(TableStatus status) {
        List<DiningTable> tables = status == null
                ? diningTableRepository.findAllByOrderByTableNumberAsc()
                : diningTableRepository.findByStatusOrderByTableNumberAsc(status);
        return tables.stream().map(this::toResponse).toList();
    }

    @Override
    public DiningTable requireByNumber(String tableNumber) {
        String normalized = requireTableNumber(tableNumber);
        return diningTableRepository.findByTableNumberIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Table " + normalized + " not found"));
    }

    @Override
    @Transactional
    public TableSeating claimForOrder(TableOrderClaim claim) {
        DiningTable table = tableLifecycle.lockByNumber(requireTableNumber(claim.tableNumber()));
        String number = table.getTableNumber();
        TableStatus status = table.getStatus();

        if (status == TableStatus.OCCUPIED) {
            // Another round for the party at the table: the customer is ordering from the table itself, or
            // staff said so. Anything else would put a second party's order on an occupied table.
            if (claim.fromCustomer() || claim.forSeatedParty()) {
                return join(table, claim, null);
            }
            return join(table, claim, overrideOrThrow(claim, "Table " + number + " is occupied. Pick a free table, "
                    + "or add this order to the party already seated there."));
        }

        Optional<Reservation> hold = tableLifecycle.holdingReservation(table);
        if (status == TableStatus.RESERVED || hold.isPresent()) {
            if (claim.fromCustomer()) {
                throw new ConflictException("Table " + number + " is reserved. Please ask our staff to seat you.");
            }
            String message = hold
                    .map(reservation -> "Table " + number + " is reserved for " + tableLifecycle.describe(reservation)
                            + ". Check the booking in from Tables, or pick another table.")
                    .orElse("Table " + number + " is reserved. Pick another table.");
            return seat(table, claim, overrideOrThrow(claim, message));
        }

        // A table waiting to be cleaned can be reopened by the party still sitting there (a customer ordering
        // from it, or staff confirming it), but a new walk-in has to wait until it is marked clean.
        if (status == TableStatus.CLEANING && !claim.fromCustomer() && !claim.forSeatedParty()) {
            return seat(table, claim, overrideOrThrow(claim, "Table " + number + " still needs cleaning. "
                    + "Mark it clean first, or pick another table."));
        }
        return seat(table, claim, null);
    }

    @Override
    @Transactional
    public void linkOrder(TableSeating seating, UUID orderId) {
        if (seating != null && seating.auditLog() != null) {
            seating.auditLog().setOrderId(orderId);
            tableAuditLogRepository.save(seating.auditLog());
        }
    }

    @Override
    @Transactional
    public void releaseAfterOrder(UUID tableId, UUID orderId, UUID actorId) {
        if (tableId == null) {
            return;
        }
        Optional<DiningTable> locked = diningTableRepository.findByIdForUpdate(tableId);
        if (locked.isEmpty() || locked.get().getStatus() != TableStatus.OCCUPIED
                || tableLifecycle.activeOrderCount(tableId) > 0) {
            return;
        }
        DiningTable table = locked.get();
        tableLifecycle.completeSeatedReservations(table);
        tableLifecycle.move(table, TableStatus.CLEANING, 0, TableAuditAction.ORDERS_FINISHED, actorId,
                Audit.forOrder(orderId));
    }

    @Override
    public List<TableAuditLogResponse> history(UUID id) {
        findById(id);
        List<TableAuditLog> logs = tableAuditLogRepository.findByTableIdOrderByCreatedAtDesc(
                id, PageRequest.of(0, HISTORY_LIMIT));
        Set<UUID> actorIds = new HashSet<>();
        for (TableAuditLog log : logs) {
            if (log.getActorId() != null) {
                actorIds.add(log.getActorId());
            }
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);
        return logs.stream()
                .map(log -> new TableAuditLogResponse(log.getId(), log.getAction(), log.getFromStatus(),
                        log.getToStatus(), Objects.requireNonNullElse(log.getGuestCount(), 0),
                        log.getActorId() != null ? actors.get(log.getActorId()) : null, log.isOverride(),
                        log.getReason(), log.getOrderId(), log.getReservationId(), log.getCreatedAt()))
                .toList();
    }

    private TableSeating seat(DiningTable table, TableOrderClaim claim, String overrideReason) {
        TableAuditLog log = tableLifecycle.move(table, TableStatus.OCCUPIED, Math.max(table.getGuestCount(), 1),
                TableAuditAction.ORDER_SEATED, claim.actorId(), Audit.none().withOverride(overrideReason));
        return new TableSeating(table, log);
    }

    private TableSeating join(DiningTable table, TableOrderClaim claim, String overrideReason) {
        TableAuditLog log = tableLifecycle.move(table, TableStatus.OCCUPIED, Math.max(table.getGuestCount(), 1),
                TableAuditAction.ORDER_ADDED, claim.actorId(), Audit.none().withOverride(overrideReason));
        return new TableSeating(table, log);
    }

    private String overrideOrThrow(TableOrderClaim claim, String message) {
        return tableLifecycle.requireOverride(claim.override(), claim.mayOverride(), claim.overrideReason(), message);
    }

    private String requireTableNumber(String tableNumber) {
        if (!StringUtils.hasText(tableNumber)) {
            throw new InvalidOperationException("Please enter your table number");
        }
        return normalize(tableNumber);
    }

    private String normalize(String tableNumber) {
        return tableNumber.strip().toUpperCase(Locale.ROOT);
    }

    private TableResponse toResponse(DiningTable table) {
        return diningTableMapper.toResponse(table, scanUrl(table));
    }

    private String scanUrl(DiningTable table) {
        String base = tableProperties.getScanBaseUrl();
        return (base.endsWith("/") ? base : base + "/") + table.getTableNumber();
    }

    private DiningTable findById(UUID id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));
    }
}
