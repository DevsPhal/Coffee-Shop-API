package org.group1.coffeeshopapi.categories.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.group1.coffeeshopapi.categories.dto.request.CategoryCreateRequest;
import org.group1.coffeeshopapi.categories.dto.request.CategoryUpdateRequest;
import org.group1.coffeeshopapi.categories.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.categories.entity.Category;
import org.group1.coffeeshopapi.categories.mapper.CategoryMapper;
import org.group1.coffeeshopapi.categories.repository.CategoryRepository;
import org.group1.coffeeshopapi.categories.service.CategoryService;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category with code " + request.getCode() + " already exists");
        }

        log.info("Category before saving: {}", request);
        Category category = categoryMapper.toEntity(request);
        Category savedCategory = categoryRepository.save(category);
        log.info("Category after saving: {}", savedCategory.getId());
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = findCategoryById(id);
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByCode(String code) {
        Category category = categoryRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with code: " + code));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        Page<CategoryResponse> page = categoryRepository.findAll(pageable).map(categoryMapper::toResponse);
        return PageUtil.toPaginatedResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findAllActive()
                .stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse updateCategory(UUID id, CategoryUpdateRequest request) {
        Category category = findCategoryById(id);
        categoryMapper.updateEntity(request, category);
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(UUID id) {
        Category category = findCategoryById(id);
        if (productRepository.existsByCategory_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Category " + category.getCode() + " cannot be deleted while products are assigned to it");
        }
        categoryRepository.delete(category);
    }

    private Category findCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with ID: " + id));
    }
}
