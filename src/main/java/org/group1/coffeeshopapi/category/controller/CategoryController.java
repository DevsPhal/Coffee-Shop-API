package org.group1.coffeeshopapi.category.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.category.dto.request.CreateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.request.UpdateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.service.CategoryService;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Category", description = "Admin only: manage product categories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse category = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Category created successfully.", category));
    }

    @GetMapping
    public ApiResponse<PageResponse<CategoryResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(categoryService.list(PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, categoryService.getById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Category updated successfully.", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ApiResponse.of(HttpStatus.OK, "Category deleted successfully.", null);
    }
}
