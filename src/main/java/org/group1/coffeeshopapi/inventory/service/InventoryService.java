package org.group1.coffeeshopapi.inventory.service;

import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockCutResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryService {
    InventoryResponse getByProduct(UUID productId);
    Page<InventoryResponse> list(Pageable pageable);

    StockMovementResponse stockIn(StockInRequest request, UUID performedBy);
    StockCutResponse stockCut(StockCutRequest request, UUID performedBy);

    Page<StockMovementResponse> listMovements(UUID productId, Pageable pageable);
}
