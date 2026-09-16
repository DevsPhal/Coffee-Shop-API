package org.group1.coffeeshopapi.product.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.product.dto.request.CreateProductRequest;
import org.group1.coffeeshopapi.product.dto.request.SetProductDiscountRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductImportResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.group1.coffeeshopapi.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Admin only: manage products")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;
    private final CurrentActor currentActor;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.create(request, currentActor.adminRef());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Product created successfully.", product));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(productService.list(categoryId, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, productService.getById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Product updated successfully.", productService.update(id, request, currentActor.adminRef()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ApiResponse.of(HttpStatus.OK, "Product deleted successfully.", null);
    }

    @PutMapping("/{id}/discount")
    public ApiResponse<ProductResponse> setDiscount(
            @PathVariable UUID id, @Valid @RequestBody SetProductDiscountRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Product discount set successfully.",
                productService.setDiscount(id, request, currentActor.adminRef()));
    }

    @DeleteMapping("/{id}/discount")
    public ApiResponse<ProductResponse> clearDiscount(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Product discount cleared successfully.",
                productService.clearDiscount(id, currentActor.adminRef()));
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ApiResponse<ProductResponse> uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.of(HttpStatus.OK, "Product image uploaded successfully.",
                productService.uploadImage(id, file, currentActor.adminRef()));
    }

    @DeleteMapping("/{id}/image")
    public ApiResponse<ProductResponse> removeImage(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Product image removed successfully.",
                productService.removeImage(id, currentActor.adminRef()));
    }

    // Expected columns (row 1 = header, data from row 2): name, description, sku, unit
    // (stockUnit — PACK/BOX/CARTON/PIECE), price, category name, reorder level (optional), size
    // options (optional — "name:price;name:price", e.g. "SMALL:1.25;MEDIUM:1.50;LARGE:1.75"; when
    // given it replaces the single default MEDIUM variant price would otherwise seed, so
    // price can be left blank), sell unit (optional — PLATE/BOTTLE/CAN/CUP/CARTON/PACKAGE/TANK/
    // PIECE, defaults to CUP), units per stock (optional — how many sell units one stock unit
    // yields, e.g. a CARTON of 24 CANs -> 24; defaults to 1). Valid rows are created even if
    // others fail.
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ApiResponse<ProductImportResponse> importExcel(@RequestParam("file") MultipartFile file) {
        ProductImportResponse response = productService.importFromExcel(file, currentActor.adminRef());
        return ApiResponse.of(HttpStatus.OK, "Import completed.", response);
    }
}
