package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.InventoryPatchRequest;
import org.group1.coffeeshopapi.admin.dto.request.InventoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.InventoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryService {

    InventoryResponse create(InventoryRequest request);

    InventoryResponse update(UUID id, InventoryRequest request);

    InventoryResponse patch(UUID id, InventoryPatchRequest request);

    void delete(UUID id);

    InventoryResponse getById(UUID id);

    InventoryResponse getByProductId(UUID productId);

    Page<InventoryResponse> getAll(Pageable pageable);
}