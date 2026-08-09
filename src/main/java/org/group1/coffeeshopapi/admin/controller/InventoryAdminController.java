package org.group1.coffeeshopapi.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.InventoryPatchRequest;
import org.group1.coffeeshopapi.admin.dto.request.InventoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.admin.service.InventoryService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryAdminController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> create(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<InventoryResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Inventory created successfully")
                        .timeStamp(LocalDateTime.now())
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
                        .timeStamp(LocalDateTime.now())
                        .data(response)
                        .build()
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryResponse>> patch(@PathVariable UUID id,
                                                                 @Valid @RequestBody InventoryPatchRequest request) {
        InventoryResponse response = inventoryService.patch(id, request);
        return ResponseEntity.ok(
                ApiResponse.<InventoryResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Inventory updated successfully")
                        .timeStamp(LocalDateTime.now())
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
                        .timeStamp(LocalDateTime.now())
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
                        .timeStamp(LocalDateTime.now())
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
                        .timeStamp(LocalDateTime.now())
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryResponse>>> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        Page<InventoryResponse> result = inventoryService.getAll(pageable);
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<InventoryResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Inventory list retrieved successfully")
                        .timeStamp(LocalDateTime.now())
                        .data(PageUtil.toPageResponse(result))
                        .build()
        );
    }
}