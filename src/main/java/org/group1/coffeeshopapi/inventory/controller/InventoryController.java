package org.group1.coffeeshopapi.inventory.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.inventory.dto.request.InventoryAdjustRequest;
import org.group1.coffeeshopapi.inventory.dto.request.InventoryUpdateRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryResponse> getAllInventory() {
        return inventoryService.getAllInventory();
    }

    @GetMapping("/low-stock")
    public List<InventoryResponse> getLowStockInventory() {
        return inventoryService.getLowStockInventory();
    }

    @GetMapping("/product/{productId}")
    public InventoryResponse getInventoryByProductId(@PathVariable UUID productId) {
        return inventoryService.getInventoryByProductId(productId);
    }

    @GetMapping("/product-code/{productCode}")
    public InventoryResponse getInventoryByProductCode(@PathVariable String productCode) {
        return inventoryService.getInventoryByProductCode(productCode);
    }

    @PostMapping("/product/{productId}/adjust")
    public InventoryResponse adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryAdjustRequest request) {
        return inventoryService.adjustStock(productId, request);
    }

    @PutMapping("/product/{productId}")
    public InventoryResponse updateInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        return inventoryService.updateInventory(productId, request);
    }
}
