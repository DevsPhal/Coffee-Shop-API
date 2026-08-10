package org.group1.coffeeshopapi.inventory.service;

import java.util.List;

import org.group1.coffeeshopapi.inventory.dto.request.InventoryAdjustRequest;
import org.group1.coffeeshopapi.inventory.dto.request.InventoryUpdateRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;

public interface InventoryService {

    List<InventoryResponse> getAllInventory();

    List<InventoryResponse> getLowStockInventory();

    InventoryResponse getInventoryByProductId(Long productId);

    InventoryResponse getInventoryByProductCode(String productCode);

    InventoryResponse adjustStock(Long productId, InventoryAdjustRequest request);

    InventoryResponse updateInventory(Long productId, InventoryUpdateRequest request);
}
