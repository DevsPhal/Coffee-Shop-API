package org.group1.coffeeshopapi.category.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.category.dto.response.CustomerCategoryResponse;
import org.group1.coffeeshopapi.category.service.CategoryService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Browsing the menu itself is customer-only in this app (see CustomerProductController) — a
// customer has to build the same categoryId filter it accepts, so this needs the same access.
@RestController
@RequestMapping("/api/customer/categories")
@RequiredArgsConstructor
@Tag(name = "Customer Categories", description = "Customer only: active categories, for filtering the menu")
@SecurityRequirement(name = "bearerAuth")
public class CustomerCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CustomerCategoryResponse>> list() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, categoryService.listActive());
    }
}
