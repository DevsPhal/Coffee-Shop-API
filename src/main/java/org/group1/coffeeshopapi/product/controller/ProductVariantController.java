package org.group1.coffeeshopapi.product.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.product.dto.request.CreateProductVariantRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductVariantRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductVariantResponse;
import org.group1.coffeeshopapi.product.service.ProductVariantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products/{productId}/variants")
@RequiredArgsConstructor
@Tag(name = "Product Variants", description = "Admin only: manage a product's variants (each with its own price add-on)")
@SecurityRequirement(name = "bearerAuth")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductVariantResponse>> create(
            @PathVariable UUID productId, @Valid @RequestBody CreateProductVariantRequest request) {
        ProductVariantResponse response = variantService.create(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Variant created successfully.", response));
    }

    @GetMapping
    public ApiResponse<List<ProductVariantResponse>> list(@PathVariable UUID productId) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, variantService.list(productId));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductVariantResponse> update(
            @PathVariable UUID productId, @PathVariable UUID id,
            @Valid @RequestBody UpdateProductVariantRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Variant updated successfully.",
                variantService.update(productId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID productId, @PathVariable UUID id) {
        variantService.delete(productId, id);
        return ApiResponse.of(HttpStatus.OK, "Variant deleted successfully.", null);
    }
}
