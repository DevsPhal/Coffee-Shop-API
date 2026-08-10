package org.group1.coffeeshopapi.products.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.products.dto.request.ProductCreateRequest;
import org.group1.coffeeshopapi.products.dto.request.ProductUpdateRequest;
import org.group1.coffeeshopapi.products.dto.response.ProductResponse;
import org.group1.coffeeshopapi.products.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductV1Controller {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ProductResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Product created successfully")
                        .data(response)
                        .timeStamp(LocalDateTime.now())
                        .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> responses = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Products retrieved successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllActiveProducts() {
        List<ProductResponse> responses = productService.getAllActiveProducts();
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Active products retrieved successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Product retrieved successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/product-id/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductByProductId(@PathVariable String productId) {
        ProductResponse response = productService.getProductByProductId(productId);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Product retrieved successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/category/{categoryCode}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(@PathVariable String categoryCode) {
        List<ProductResponse> responses = productService.getProductsByCategory(categoryCode);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Products retrieved by category successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(@RequestParam String name) {
        List<ProductResponse> responses = productService.searchProductsByName(name);
        return ResponseEntity.ok(ApiResponse.<List<ProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Products searched successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.<ProductResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Product updated successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Product deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/product-id/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteProductByProductId(@PathVariable String productId) {
        productService.deleteProductByProductId(productId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Product deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}