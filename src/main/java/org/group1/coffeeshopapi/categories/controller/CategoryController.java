package org.group1.coffeeshopapi.categories.controller;

import java.util.List;
import java.util.UUID;

import org.group1.coffeeshopapi.categories.dto.request.CategoryCreateRequest;
import org.group1.coffeeshopapi.categories.dto.request.CategoryUpdateRequest;
import org.group1.coffeeshopapi.categories.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.categories.service.CategoryService;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
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
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse createCategory(@Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.createCategory(request);
    }

    @GetMapping
    public PaginatedResponse<CategoryResponse> getAllCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        return categoryService.getAllCategories(pageable);
    }

    @GetMapping("/active")
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryService.getAllActiveCategories();
    }

    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable UUID id) {
        return categoryService.getCategoryById(id);
    }

    @GetMapping("/code/{code}")
    public CategoryResponse getCategoryByCode(@PathVariable String code) {
        return categoryService.getCategoryByCode(code);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
    }
}
