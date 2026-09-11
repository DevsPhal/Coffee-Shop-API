package org.group1.coffeeshopapi.extra.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.extra.dto.request.AttachProductExtraRequest;
import org.group1.coffeeshopapi.extra.dto.request.UpdateProductExtraRequest;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;
import org.group1.coffeeshopapi.extra.service.ProductExtraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products/{productId}/extras")
@RequiredArgsConstructor
@Tag(name = "Product Extras", description = "Admin only: choose which extras (e.g. Pearl) a product offers — the customer then opts to add or not add each one")
@SecurityRequirement(name = "bearerAuth")
public class ProductExtraController {

    private final ProductExtraService productExtraService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductExtraResponse>> attach(
            @PathVariable UUID productId, @Valid @RequestBody AttachProductExtraRequest request) {
        ProductExtraResponse response = productExtraService.attach(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Extra offered on this product.", response));
    }

    @GetMapping
    public ApiResponse<List<ProductExtraResponse>> list(@PathVariable UUID productId) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, productExtraService.list(productId));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductExtraResponse> update(
            @PathVariable UUID productId, @PathVariable UUID id,
            @Valid @RequestBody UpdateProductExtraRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Product extra updated successfully.",
                productExtraService.update(productId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> detach(@PathVariable UUID productId, @PathVariable UUID id) {
        productExtraService.detach(productId, id);
        return ApiResponse.of(HttpStatus.OK, "Extra removed from this product.", null);
    }
}
