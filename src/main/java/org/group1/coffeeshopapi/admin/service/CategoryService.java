package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.CategoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.CategoryResponse;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(UUID id, CategoryRequest request);

    void delete(UUID id);

    CategoryResponse getById(UUID id);

    List<CategoryResponse> getAll();
}