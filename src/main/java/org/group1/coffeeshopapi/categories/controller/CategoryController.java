package org.group1.coffeeshopapi.categories.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.categories.dto.request.CategoryCreateRequest;
import org.group1.coffeeshopapi.categories.dto.request.CategoryUpdateRequest;
import org.group1.coffeeshopapi.categories.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.categories.service.CategoryService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CategoryResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Category created successfully")
                        .data(response)
                        .timeStamp(LocalDateTime.now())
                        .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> responses = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Categories retrieved successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllActiveCategories() {
        List<CategoryResponse> responses = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Active categories retrieved successfully")
                .data(responses)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryByCode(@PathVariable String code) {
        CategoryResponse response = categoryService.getCategoryByCode(code);
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Category retrieved successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.<CategoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Category updated successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Category deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
