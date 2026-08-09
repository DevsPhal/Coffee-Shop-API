package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.CategoryPatchRequest;
import org.group1.coffeeshopapi.admin.dto.request.CategoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(UUID id, CategoryRequest request);

    CategoryResponse patch(UUID id, CategoryPatchRequest request);

    void delete(UUID id);

    CategoryResponse getById(UUID id);

    Page<CategoryResponse> getAll(Pageable pageable);
}