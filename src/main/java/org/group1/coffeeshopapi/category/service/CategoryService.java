package org.group1.coffeeshopapi.category.service;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.dto.request.CreateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.request.UpdateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.dto.response.CustomerCategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

// actorAdmin is null when the Super Admin is the one acting — see CurrentActor.adminRef().
public interface CategoryService {
    CategoryResponse create(CreateCategoryRequest request, Admin actorAdmin);
    CategoryResponse getById(UUID id);
    Page<CategoryResponse> list(Pageable pageable);
    CategoryResponse update(UUID id, UpdateCategoryRequest request, Admin actorAdmin);
    void delete(UUID id);

    // Public/customer-facing: active categories only, no staff audit identities — what a
    // customer app browses to build a product category filter.
    List<CustomerCategoryResponse> listActive();
}
