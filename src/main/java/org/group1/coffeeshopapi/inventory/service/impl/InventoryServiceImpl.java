package org.group1.coffeeshopapi.inventory.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.inventory.dto.request.InventoryAdjustRequest;
import org.group1.coffeeshopapi.inventory.dto.request.InventoryUpdateRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.mapper.InventoryMapper;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventory() {
        return inventoryRepository.findAllLowStock()
                .stream()
                .map(inventoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        return inventoryMapper.toResponse(findByProductId(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductCode(String productCode) {
        Inventory inventory = inventoryRepository.findByProduct_ProductId(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product code: " + productCode));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse adjustStock(Long productId, InventoryAdjustRequest request) {
        Inventory inventory = findByProductId(productId);

        int newQuantity = inventory.getQuantityOnHand() + request.getQuantityChange();
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock adjustment would result in negative quantity ("
                    + inventory.getQuantityOnHand() + " + " + request.getQuantityChange() + ")");
        }

        inventory.setQuantityOnHand(newQuantity);
        if (request.getQuantityChange() > 0) {
            inventory.setLastRestockedAt(LocalDateTime.now());
        }

        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    @Override
    public InventoryResponse updateInventory(Long productId, InventoryUpdateRequest request) {
        Inventory inventory = findByProductId(productId);

        if (request.getQuantityOnHand() != null) {
            inventory.setQuantityOnHand(request.getQuantityOnHand());
        }
        if (request.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }

        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    private Inventory findByProductId(Long productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));
    }
}
