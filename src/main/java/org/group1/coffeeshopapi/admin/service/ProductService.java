package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.ProductPatchRequest;
import org.group1.coffeeshopapi.admin.dto.request.ProductRequest;
import org.group1.coffeeshopapi.admin.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    ProductResponse update(UUID id, ProductRequest request);

    ProductResponse patch(UUID id, ProductPatchRequest request);

    void delete(UUID id);

    ProductResponse getById(UUID id);

    Page<ProductResponse> getAll(Pageable pageable);

    Page<ProductResponse> getByCategory(UUID categoryId, Pageable pageable);
}