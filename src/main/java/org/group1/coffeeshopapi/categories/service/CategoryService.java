package org.group1.coffeeshopapi.categories.service;

import java.util.List;
import java.util.UUID;

import org.group1.coffeeshopapi.categories.dto.request.CategoryCreateRequest;
import org.group1.coffeeshopapi.categories.dto.request.CategoryUpdateRequest;
import org.group1.coffeeshopapi.categories.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreateRequest request);

    CategoryResponse getCategoryById(UUID id);

    CategoryResponse getCategoryByCode(String code);

    PaginatedResponse<CategoryResponse> getAllCategories(Pageable pageable);

    List<CategoryResponse> getAllActiveCategories();

    CategoryResponse updateCategory(UUID id, CategoryUpdateRequest request);

    void deleteCategory(UUID id);
}
