package org.group1.coffeeshopapi.inventory.controller;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.inventory.dto.request.InventoryAdjustRequest;
import org.group1.coffeeshopapi.inventory.dto.request.InventoryUpdateRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory() {
        List<InventoryResponse> responses = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Inventory retrieved successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockInventory() {
        List<InventoryResponse> responses = inventoryService.getLowStockInventory();
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Low stock inventory retrieved successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProductId(@PathVariable UUID productId) {
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Inventory retrieved successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/product-code/{productCode}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProductCode(@PathVariable String productCode) {
        InventoryResponse response = inventoryService.getInventoryByProductCode(productCode);
        return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Inventory retrieved successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/product/{productId}/adjust")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryAdjustRequest request) {
        InventoryResponse response = inventoryService.adjustStock(productId, request);
        return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Stock adjusted successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        InventoryResponse response = inventoryService.updateInventory(productId, request);
        return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Inventory updated successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
