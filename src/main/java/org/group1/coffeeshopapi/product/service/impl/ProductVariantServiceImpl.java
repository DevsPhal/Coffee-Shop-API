package org.group1.coffeeshopapi.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.product.dto.request.CreateProductVariantRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductVariantRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductVariantResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.mapper.ProductVariantMapper;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.product.service.ProductVariantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper variantMapper;

    @Override
    @Transactional
    public ProductVariantResponse create(UUID productId, CreateProductVariantRequest request) {
        Product product = findProduct(productId);
        if (variantRepository.existsByProductIdAndName(productId, request.name())) {
            throw new DuplicateResourceException("This product already has a variant named '" + request.name() + "'");
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName(request.name());
        variant.setPrice(request.price());
        variant.setSortOrder(request.sortOrder());

        return variantMapper.toResponse(variantRepository.save(variant));
    }

    @Override
    public List<ProductVariantResponse> list(UUID productId) {
        findProduct(productId);
        return variantRepository.findByProductIdOrderBySortOrderAscNameAsc(productId).stream()
                .map(variantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductVariantResponse update(UUID productId, UUID id, UpdateProductVariantRequest request) {
        ProductVariant variant = findByIdAndProduct(productId, id);

        if (request.name() != null) {
            if (!request.name().equals(variant.getName())
                    && variantRepository.existsByProductIdAndName(productId, request.name())) {
                throw new DuplicateResourceException("This product already has a variant named '" + request.name() + "'");
            }
            variant.setName(request.name());
        }
        if (request.price() != null) {
            variant.setPrice(request.price());
        }
        if (request.sortOrder() != null) {
            variant.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            variant.setStatus(request.status());
        }

        return variantMapper.toResponse(variantRepository.save(variant));
    }

    @Override
    @Transactional
    public void delete(UUID productId, UUID id) {
        variantRepository.delete(findByIdAndProduct(productId, id));
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private ProductVariant findByIdAndProduct(UUID productId, UUID id) {
        return variantRepository.findByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
    }
}
