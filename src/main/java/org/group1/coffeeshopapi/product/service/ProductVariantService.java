package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.product.dto.request.CreateProductVariantRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductVariantRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductVariantResponse;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {
    ProductVariantResponse create(UUID productId, CreateProductVariantRequest request);
    List<ProductVariantResponse> list(UUID productId);
    ProductVariantResponse update(UUID productId, UUID id, UpdateProductVariantRequest request);
    void delete(UUID productId, UUID id);
}
