package org.group1.coffeeshopapi.products.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.group1.coffeeshopapi.categories.entity.Category;
import org.group1.coffeeshopapi.categories.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
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
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.findByProductId(request.getProductId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product with ID " + request.getProductId() + " already exists");
        }

        log.info("Product before saving: {}", request);
        Product product = productMapper.toEntity(request);
        product.setCategory(findCategoryByCode(request.getCategoryCode()));
        Product savedProduct = productRepository.save(product);
        log.info("Product after saving: {}", savedProduct.getId());
        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    public ProductResponse getProductByProductId(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found with Product ID: " + productId));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<ProductResponse> getAllProducts(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAll(pageable).map(productMapper::toResponse);
        return PageUtil.toPaginatedResponse(page);
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found with ID: " + id));
        product.setIsActive(false);
        productRepository.save(product);
    }

    @Override
    public void deleteProductByProductId(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product not found with Product ID: " + productId));
        product.setIsActive(false);
        productRepository.save(product);
    }

    private Category findCategoryByCode(String categoryCode) {
        return categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Category not found with code: " + categoryCode));
    }
}
