package org.group1.coffeeshopapi.products.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.group1.coffeeshopapi.categories.entity.Category;
import org.group1.coffeeshopapi.categories.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.group1.coffeeshopapi.products.dto.request.ProductCreateRequest;
import org.group1.coffeeshopapi.products.dto.request.ProductUpdateRequest;
import org.group1.coffeeshopapi.products.dto.response.ProductResponse;
import org.group1.coffeeshopapi.products.entity.Product;
import org.group1.coffeeshopapi.products.mapper.ProductMapper;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.group1.coffeeshopapi.products.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        // Check if product ID already exists
        if (productRepository.findByProductId(request.getProductId()).isPresent()) {
            throw new DuplicateResourceException("Product with ID " + request.getProductId() + " already exists");
        }

        Product product = productMapper.toEntity(request);
        product.setCategory(findCategoryByCode(request.getCategoryCode()));
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse getProductByProductId(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with Product ID: " + productId));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAll(pageable).map(productMapper::toResponse);
        return PageUtil.toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findAllActive()
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String categoryCode) {
        return productRepository.findByCategoryCodeAndActive(categoryCode)
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCaseAndActive(name)
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        productMapper.updateEntity(request, product);
        if (request.getCategoryCode() != null) {
            product.setCategory(findCategoryByCode(request.getCategoryCode()));
        }
        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        productRepository.delete(product);
    }

    @Override
    public void deleteProductByProductId(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with Product ID: " + productId));
        productRepository.delete(product);
    }

    private Category findCategoryByCode(String categoryCode) {
        return categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with code: " + categoryCode));
    }
}
