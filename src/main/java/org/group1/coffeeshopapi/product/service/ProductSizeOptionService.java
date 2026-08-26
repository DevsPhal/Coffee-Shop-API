package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.product.dto.request.CreateProductSizeOptionRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductSizeOptionRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductSizeOptionResponse;

import java.util.List;
import java.util.UUID;

public interface ProductSizeOptionService {
    ProductSizeOptionResponse create(UUID productId, CreateProductSizeOptionRequest request);
    List<ProductSizeOptionResponse> list(UUID productId);
    ProductSizeOptionResponse update(UUID productId, UUID id, UpdateProductSizeOptionRequest request);
    void delete(UUID productId, UUID id);
}
