package org.group1.coffeeshopapi.products.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.products.dto.request.ProductCreateRequest;
import org.group1.coffeeshopapi.products.dto.request.ProductUpdateRequest;
import org.group1.coffeeshopapi.products.dto.response.ProductResponse;
import org.group1.coffeeshopapi.products.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductV1Controller {

    private final ProductService productService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse createProduct(@Valid @RequestBody ProductCreateRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping
    public PaginatedResponse<ProductResponse> getAllProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        return productService.getAllProducts(pageable);
    }

    @GetMapping("/active")
    public List<ProductResponse> getAllActiveProducts() {
        return productService.getAllActiveProducts();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return productService.getProductById(id);
    }

    @GetMapping("/product-id/{productId}")
    public ProductResponse getProductByProductId(@PathVariable String productId) {
        return productService.getProductByProductId(productId);
    }

    @GetMapping("/category/{categoryCode}")
    public List<ProductResponse> getProductsByCategory(@PathVariable String categoryCode) {
        return productService.getProductsByCategory(categoryCode);
    }

    @GetMapping("/search")
    public List<ProductResponse> searchProducts(@RequestParam String name) {
        return productService.searchProductsByName(name);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequest request) {
        return productService.updateProduct(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/product-id/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProductByProductId(@PathVariable String productId) {
        productService.deleteProductByProductId(productId);
    }
}
