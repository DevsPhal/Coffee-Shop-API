package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.ProductRequest;
import org.group1.coffeeshopapi.admin.dto.response.ProductResponse;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    void delete(UUID id);

    ProductResponse getById(UUID id);

    List<ProductResponse> getAll();

    List<ProductResponse> getByCategory(UUID categoryId);
}