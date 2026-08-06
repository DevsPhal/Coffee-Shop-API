package org.group1.coffeeshopapi.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.repository.CategoryRepository;
import org.group1.coffeeshopapi.admin.entity.Category;
import org.group1.coffeeshopapi.admin.entity.Product;
import org.group1.coffeeshopapi.admin.dto.request.ProductRequest;
import org.group1.coffeeshopapi.admin.dto.response.ProductResponse;
import org.group1.coffeeshopapi.admin.mapper.ProductMapper;
import org.group1.coffeeshopapi.admin.repository.ProductRepository;
import org.group1.coffeeshopapi.admin.service.ProductService;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        Product product = productMapper.toEntity(request, category);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));
        productMapper.updateEntity(product, request, category);
        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        productRepository.delete(product);
    }

    @Override
    public ProductResponse getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponse> getAll() {
        return productRepository.findAll()
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getByCategory(UUID categoryId) {
        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(productMapper::toResponse)
                .toList();
    }
}