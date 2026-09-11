package org.group1.coffeeshopapi.extra.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.extra.dto.request.AttachProductExtraRequest;
import org.group1.coffeeshopapi.extra.dto.request.UpdateProductExtraRequest;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.extra.mapper.ProductExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.extra.service.ProductExtraService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductExtraServiceImpl implements ProductExtraService {

    private final ProductExtraRepository productExtraRepository;
    private final ProductRepository productRepository;
    private final ExtraRepository extraRepository;
    private final ProductExtraMapper productExtraMapper;

    @Override
    @Transactional
    public ProductExtraResponse attach(UUID productId, AttachProductExtraRequest request) {
        Product product = findProduct(productId);
        Extra extra = findExtra(request.extraId());
        if (productExtraRepository.existsByProductIdAndExtraId(productId, request.extraId())) {
            throw new DuplicateResourceException(
                    "'" + extra.getName() + "' is already offered on '" + product.getName() + "'");
        }

        ProductExtra productExtra = new ProductExtra();
        productExtra.setProduct(product);
        productExtra.setExtra(extra);
        productExtra.setSortOrder(request.sortOrder());

        return productExtraMapper.toResponse(productExtraRepository.save(productExtra));
    }

    @Override
    public List<ProductExtraResponse> list(UUID productId) {
        findProduct(productId);
        return productExtraRepository.findByProductIdOrderBySortOrderAscId(productId).stream()
                .map(productExtraMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductExtraResponse update(UUID productId, UUID id, UpdateProductExtraRequest request) {
        ProductExtra productExtra = findByIdAndProduct(productId, id);

        if (request.sortOrder() != null) {
            productExtra.setSortOrder(request.sortOrder());
        }
        if (request.status() != null) {
            productExtra.setStatus(request.status());
        }

        return productExtraMapper.toResponse(productExtraRepository.save(productExtra));
    }

    @Override
    @Transactional
    public void detach(UUID productId, UUID id) {
        productExtraRepository.delete(findByIdAndProduct(productId, id));
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Extra findExtra(UUID extraId) {
        return extraRepository.findById(extraId)
                .orElseThrow(() -> new ResourceNotFoundException("Extra not found"));
    }

    private ProductExtra findByIdAndProduct(UUID productId, UUID id) {
        return productExtraRepository.findByIdAndProductId(id, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Extra attachment not found"));
    }
}
