package org.group1.coffeeshopapi.category.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.dto.request.CreateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.request.UpdateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.dto.response.CustomerCategoryResponse;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.mapper.CategoryMapper;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.category.service.CategoryService;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request, Admin actorAdmin) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setCategoryGroup(request.categoryGroup());
        category.setCreatedByAdmin(actorAdmin);
        category.setUpdatedByAdmin(actorAdmin);
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<CategoryResponse> list(Pageable pageable) {
        // createdByAdmin/updatedByAdmin are batched by Hibernate itself.
        return categoryRepository.findAll(pageable).map(categoryMapper::toResponse);
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request, Admin actorAdmin) {
        Category category = findById(id);

        if (request.name() != null) {
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
                throw new DuplicateResourceException("A category with this name already exists");
            }
            category.setName(request.name());
        }
        if (request.description() != null) {
            category.setDescription(request.description());
        }
        if (request.status() != null) {
            category.setStatus(request.status());
        }
        if (request.categoryGroup() != null) {
            category.setCategoryGroup(request.categoryGroup());
        }
        category.setUpdatedByAdmin(actorAdmin);

        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = findById(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new InvalidOperationException("Cannot delete a category that still has products assigned to it");
        }
        categoryRepository.delete(category);
    }

    @Override
    public List<CustomerCategoryResponse> listActive() {
        return categoryRepository.findByStatusOrderByNameAsc(Status.ACTIVE).stream()
                .map(category -> categoryMapper.toCustomerResponse(toResponse(category)))
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        return categoryMapper.toResponse(category);
    }

    private Category findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
