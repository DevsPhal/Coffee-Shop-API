package org.group1.coffeeshopapi.category.controller;

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

// No auth required — builds the category filter on the public menu.
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public: active categories, for filtering the menu")
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CustomerCategoryResponse>> list() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, categoryService.listActive());
    }
}
