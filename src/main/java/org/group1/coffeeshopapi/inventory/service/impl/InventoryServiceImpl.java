package org.group1.coffeeshopapi.inventory.service.impl;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.group1.coffeeshopapi.inventory.dto.request.InventoryAdjustRequest;
import org.group1.coffeeshopapi.inventory.dto.request.InventoryUpdateRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.mapper.InventoryMapper;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
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
    public InventoryResponse getInventoryByProductId(UUID productId) {
        return inventoryMapper.toResponse(findByProductId(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductCode(String productCode) {
        Inventory inventory = inventoryRepository.findByProduct_ProductId(productCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inventory not found for product code: " + productCode));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse adjustStock(UUID productId, InventoryAdjustRequest request) {
        Inventory inventory = findByProductId(productId);

        int newQuantity = inventory.getQuantityOnHand() + request.getQuantityChange();
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stock adjustment would result in negative quantity ("
                    + inventory.getQuantityOnHand() + " + " + request.getQuantityChange() + ")");
        }

        log.info("Inventory before stock adjustment: productId={}, quantity={}", productId, inventory.getQuantityOnHand());
        inventory.setQuantityOnHand(newQuantity);
        if (request.getQuantityChange() > 0) {
            inventory.setLastRestockedAt(LocalDateTime.now());
        }

        Inventory saved = inventoryRepository.save(inventory);
        log.info("Inventory after stock adjustment: productId={}, quantity={}", productId, saved.getQuantityOnHand());
        return inventoryMapper.toResponse(saved);
    }

    @Override
    public InventoryResponse updateInventory(UUID productId, InventoryUpdateRequest request) {
        Inventory inventory = findByProductId(productId);

        if (request.getQuantityOnHand() != null) {
            if (request.getQuantityOnHand() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
            }
            inventory.setQuantityOnHand(request.getQuantityOnHand());
        }
        if (request.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        }

        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    private Inventory findByProductId(UUID productId) {
        return inventoryRepository.findByProduct_Id(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Inventory not found for product ID: " + productId));
    }
}
