package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.InventoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.InventoryResponse;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    InventoryResponse create(InventoryRequest request);

    InventoryResponse update(UUID id, InventoryRequest request);

    void delete(UUID id);

    InventoryResponse getById(UUID id);

    InventoryResponse getByProductId(UUID productId);

    List<InventoryResponse> getAll();
}