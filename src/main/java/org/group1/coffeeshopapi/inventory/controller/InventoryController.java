package org.group1.coffeeshopapi.inventory.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockCutResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Admin only: stock levels and FIFO/LIFO stock-in/stock-cut processing")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<PageResponse<InventoryResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(inventoryService.list(PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{productId}")
    public ApiResponse<InventoryResponse> getByProduct(@PathVariable UUID productId) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, inventoryService.getByProduct(productId));
    }

    @GetMapping("/{productId}/movements")
    public ApiResponse<PageResponse<StockMovementResponse>> listMovements(
            @PathVariable UUID productId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(inventoryService.listMovements(productId, PageUtil.buildPageable(page, size))));
    }

    @PostMapping("/stock-in")
    public ApiResponse<StockMovementResponse> stockIn(
            @Valid @RequestBody StockInRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        StockMovementResponse response = inventoryService.stockIn(request, currentUser.getId());
        return ApiResponse.of(HttpStatus.OK, "Stock received successfully.", response);
    }

    @PostMapping("/stock-cut")
    public ApiResponse<StockCutResponse> stockCut(
            @Valid @RequestBody StockCutRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        StockCutResponse response = inventoryService.stockCut(request, currentUser.getId());
        return ApiResponse.of(HttpStatus.OK, "Stock cut successfully.", response);
    }
}
