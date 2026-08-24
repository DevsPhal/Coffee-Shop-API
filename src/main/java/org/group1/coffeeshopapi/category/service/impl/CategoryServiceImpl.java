package org.group1.coffeeshopapi.category.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.category.dto.request.CreateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.request.UpdateCategoryRequest;
import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.mapper.CategoryMapper;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.category.service.CategoryService;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final ActorLookupService actorLookupService;

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request, UUID actorId) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setCreatedBy(actorId);
        category.setUpdatedBy(actorId);
        return toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<CategoryResponse> list(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);

        Set<UUID> actorIds = new HashSet<>();
        for (Category category : categories) {
            actorIds.add(category.getCreatedBy());
            actorIds.add(category.getUpdatedBy());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);

        return categories.map(category -> categoryMapper.toResponse(category,
                actors.get(category.getCreatedBy()), actors.get(category.getUpdatedBy())));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request, UUID actorId) {
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
        category.setUpdatedBy(actorId);

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

    private CategoryResponse toResponse(Category category) {
        return categoryMapper.toResponse(category,
                actorLookupService.resolve(category.getCreatedBy()),
                actorLookupService.resolve(category.getUpdatedBy()));
    }

    private Category findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
