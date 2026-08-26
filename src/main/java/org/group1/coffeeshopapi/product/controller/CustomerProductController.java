package org.group1.coffeeshopapi.product.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.product.dto.response.CustomerProductResponse;
import org.group1.coffeeshopapi.product.mapper.ProductMapper;
import org.group1.coffeeshopapi.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/customer/products")
@RequiredArgsConstructor
@Tag(name = "Customer Products", description = "Customer only: browse the menu")
@SecurityRequirement(name = "bearerAuth")
public class CustomerProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping
    public ApiResponse<PageResponse<CustomerProductResponse>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(productService.listActive(categoryId, PageUtil.buildPageable(page, size))
                        .map(productMapper::toCustomerResponse)));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerProductResponse> getById(@PathVariable UUID id) {
        var product = productService.getById(id);
        // getById is shared with the admin catalog and doesn't filter by status — a customer
        // has no reason to look up a DRAFT/INACTIVE product by id, so treat it as not found.
        if (product.status() != Status.ACTIVE) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, productMapper.toCustomerResponse(product));
    }
}
