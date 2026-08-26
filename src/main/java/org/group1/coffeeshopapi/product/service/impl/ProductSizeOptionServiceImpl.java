package org.group1.coffeeshopapi.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.product.dto.request.CreateProductSizeOptionRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductSizeOptionRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductSizeOptionResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;
import org.group1.coffeeshopapi.product.mapper.ProductSizeOptionMapper;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductSizeOptionRepository;
import org.group1.coffeeshopapi.product.service.ProductSizeOptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductSizeOptionServiceImpl implements ProductSizeOptionService {

    private final ProductSizeOptionRepository sizeOptionRepository;
    private final ProductRepository productRepository;
    private final ProductSizeOptionMapper sizeOptionMapper;

    @Override
    @Transactional
    public ProductSizeOptionResponse create(UUID productId, CreateProductSizeOptionRequest request) {
        Product product = findProduct(productId);
        if (sizeOptionRepository.existsByProductIdAndNameIgnoreCase(productId, request.name())) {
            throw new DuplicateResourceException("This product already has a size option named '" + request.name() + "'");
        }

        ProductSizeOption sizeOption = new ProductSizeOption();
        sizeOption.setProduct(product);
        sizeOption.setName(request.name());
        sizeOption.setPriceDelta(request.priceDelta());
        sizeOption.setSortOrder(request.sortOrder());

        return sizeOptionMapper.toResponse(sizeOptionRepository.save(sizeOption));
    }

    @Override
    public List<ProductSizeOptionResponse> list(UUID productId) {
        findProduct(productId);
        return sizeOptionRepository.findByProductIdOrderBySortOrderAscNameAsc(productId).stream()
                .map(sizeOptionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductSizeOptionResponse update(UUID productId, UUID id, UpdateProductSizeOptionRequest request) {
        ProductSizeOption sizeOption = findByIdAndProduct(productId, id);

        if (request.name() != null) {
            if (!request.name().equalsIgnoreCase(sizeOption.getName())
                    && sizeOptionRepository.existsByProductIdAndNameIgnoreCase(productId, request.name())) {
                throw new DuplicateResourceException("This product already has a size option named '" + request.name() + "'");
            }
            sizeOption.setName(request.name());
        }
        if (request.priceDelta() != null) {
            sizeOption.setPriceDelta(request.priceDelta());
        }
        if (request.sortOrder() != null) {
            sizeOption.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            sizeOption.setStatus(request.status());
        }

        return sizeOptionMapper.toResponse(sizeOptionRepository.save(sizeOption));
    }

    @Override
    @Transactional
    public void delete(UUID productId, UUID id) {
        sizeOptionRepository.delete(findByIdAndProduct(productId, id));
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private ProductSizeOption findByIdAndProduct(UUID productId, UUID id) {
        return sizeOptionRepository.findByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Size option not found"));
    }
}
