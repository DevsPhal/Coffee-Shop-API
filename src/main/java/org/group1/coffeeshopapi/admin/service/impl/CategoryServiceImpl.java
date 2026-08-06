package org.group1.coffeeshopapi.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.dto.request.CategoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.admin.mapper.CategoryMapper;
import org.group1.coffeeshopapi.admin.repository.CategoryRepository;
import org.group1.coffeeshopapi.admin.service.CategoryService;
import org.group1.coffeeshopapi.admin.entity.Category;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Category already exists: " + request.getName());
        }
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        categoryMapper.updateEntity(category, request);
        Category updated = categoryRepository.save(category);
        return categoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse getById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }
}