package org.group1.coffeeshopapi.product.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.product.dto.request.CreateProductSizeOptionRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductSizeOptionRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductSizeOptionResponse;
import org.group1.coffeeshopapi.product.service.ProductSizeOptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products/{productId}/size-options")
@RequiredArgsConstructor
@Tag(name = "Product Size Options", description = "Admin only: manage a product's size variants (each with its own price add-on)")
@SecurityRequirement(name = "bearerAuth")
public class ProductSizeOptionController {

    private final ProductSizeOptionService sizeOptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductSizeOptionResponse>> create(
            @PathVariable UUID productId, @Valid @RequestBody CreateProductSizeOptionRequest request) {
        ProductSizeOptionResponse response = sizeOptionService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Size option created successfully.", response));
    }

    @GetMapping
    public ApiResponse<List<ProductSizeOptionResponse>> list(@PathVariable UUID productId) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, sizeOptionService.list(productId));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductSizeOptionResponse> update(
            @PathVariable UUID productId, @PathVariable UUID id,
            @Valid @RequestBody UpdateProductSizeOptionRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Size option updated successfully.",
                sizeOptionService.update(productId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID productId, @PathVariable UUID id) {
        sizeOptionService.delete(productId, id);
        return ApiResponse.of(HttpStatus.OK, "Size option deleted successfully.", null);
    }
}
