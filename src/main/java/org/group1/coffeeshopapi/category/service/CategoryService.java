package org.group1.coffeeshopapi.category.service;

import org.group1.coffeeshopapi.category.dto.request.CreateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.request.UpdateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {
    CategoryResponse create(CreateCategoryRequest request, UUID actorId);
    CategoryResponse getById(UUID id);
    Page<CategoryResponse> list(Pageable pageable);
    CategoryResponse update(UUID id, UpdateCategoryRequest request, UUID actorId);
    void delete(UUID id);
}
