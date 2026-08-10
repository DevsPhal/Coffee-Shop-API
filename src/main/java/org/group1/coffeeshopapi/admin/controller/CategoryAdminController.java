package org.group1.coffeeshopapi.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.CategoryPatchRequest;
import org.group1.coffeeshopapi.admin.dto.request.CategoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.admin.service.CategoryService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CategoryAdminController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<CategoryResponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Category created successfully")
                        .timeStamp(LocalDateTime.now())
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.update(id, request);
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Category updated successfully")
                        .timeStamp(LocalDateTime.now())
                        .data(response)
                        .build()
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> patch(@PathVariable UUID id,
                                                                @Valid @RequestBody CategoryPatchRequest request) {
        CategoryResponse response = categoryService.patch(id, request);
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Category updated successfully")
                        .timeStamp(LocalDateTime.now())
                        .data(response)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .status(HttpStatus.OK.value())
                        .message("Category deleted successfully")
                        .timeStamp(LocalDateTime.now())
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable UUID id) {
        CategoryResponse response = categoryService.getById(id);
        return ResponseEntity.ok(
                ApiResponse.<CategoryResponse>builder()
                        .status(HttpStatus.OK.value())
                        .message("Category retrieved successfully")
                        .timeStamp(LocalDateTime.now())
                        .data(response)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        Page<CategoryResponse> result = categoryService.getAll(pageable);
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<CategoryResponse>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Categories retrieved successfully")
                        .timeStamp(LocalDateTime.now())
                        .data(PageUtil.toPageResponse(result))
                        .build()
        );
    }
}