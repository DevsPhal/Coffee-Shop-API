package org.group1.coffeeshopapi.table.service;

import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.table.dto.request.CreateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
import org.group1.coffeeshopapi.table.dto.response.TableAuditLogResponse;
import org.group1.coffeeshopapi.table.dto.response.TableResponse;
import org.group1.coffeeshopapi.table.entity.DiningTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface DiningTableService {
    TableResponse create(CreateTableRequest request);
    TableResponse getById(UUID id);
    Page<TableResponse> list(TableStatus status, Pageable pageable);
    TableResponse update(UUID id, UpdateTableRequest request);

    // A manual status change from the tables screen, checked against TableRules. Only an admin
    // (mayOverride) can override a rule, and only with a reason; every change is audited.
    TableResponse updateStatus(UUID id, UpdateTableStatusRequest request, UUID actorId, boolean mayOverride);

    void delete(UUID id);

    TableResponse getByNumber(String tableNumber);
    List<TableResponse> listPublic(TableStatus status);

    DiningTable requireByNumber(String tableNumber);

    // Locks the table and gives it to a new order: a free table is seated; an occupied one only takes the
    // order when it is for the party already there; reserved and cleaning tables refuse walk-ins.
    TableSeating claimForOrder(TableOrderClaim claim);

    // Records which order the seating was for, once the order has an id.
    void linkOrder(TableSeating seating, UUID orderId);

    // An order at the table finished, was cancelled or moved away. When the table has no active order
    // left, the party is done: the table needs cleaning and a seated booking is completed.
    void releaseAfterOrder(UUID tableId, UUID orderId, UUID actorId);

    List<TableAuditLogResponse> history(UUID id);
}
