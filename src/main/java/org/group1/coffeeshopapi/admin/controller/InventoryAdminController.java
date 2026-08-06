package org.group1.coffeeshopapi.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.InventoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.admin.service.InventoryService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class InventoryAdminController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> create(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<InventoryResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Inventory created successfully")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> update(@PathVariable UUID id,
                                                                 @Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.update(id, request);
        return ResponseEntity.ok(
                ApiResponse.<InventoryResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Inventory updated successfully")
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        inventoryService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message("Inventory deleted successfully")
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getById(@PathVariable UUID id) {
        InventoryResponse response = inventoryService.getById(id);
        return ResponseEntity.ok(
                ApiResponse.<InventoryResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Inventory retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProductId(@PathVariable UUID productId) {
        InventoryResponse response = inventoryService.getByProductId(productId);
        return ResponseEntity.ok(
                ApiResponse.<InventoryResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Inventory retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAll() {
        List<InventoryResponse> response = inventoryService.getAll();
        return ResponseEntity.ok(
                ApiResponse.<List<InventoryResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Inventory list retrieved successfully")
                        .data(response)
                        .build()
        );
    }
}