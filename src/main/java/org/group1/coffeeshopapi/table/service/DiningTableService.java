package org.group1.coffeeshopapi.table.service;

import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.table.dto.request.CreateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableRequest;
import org.group1.coffeeshopapi.table.dto.request.UpdateTableStatusRequest;
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
    TableResponse updateStatus(UUID id, UpdateTableStatusRequest request);
    void delete(UUID id);

    TableResponse getByNumber(String tableNumber);
    List<TableResponse> listPublic(TableStatus status);

    DiningTable requireByNumber(String tableNumber);
    DiningTable seatForOrder(String tableNumber);
}
