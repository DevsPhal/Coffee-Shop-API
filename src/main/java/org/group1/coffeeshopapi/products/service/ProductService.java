package org.group1.coffeeshopapi.products.service;

import java.util.List;
import java.util.UUID;

import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.products.dto.request.ProductCreateRequest;
import org.group1.coffeeshopapi.products.dto.request.ProductUpdateRequest;
import org.group1.coffeeshopapi.products.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProductById(UUID id);

    ProductResponse getProductByProductId(String productId);

    PaginatedResponse<ProductResponse> getAllProducts(Pageable pageable);

    List<ProductResponse> getAllActiveProducts();

    List<ProductResponse> getProductsByCategory(String categoryCode);

    List<ProductResponse> searchProductsByName(String name);

    ProductResponse updateProduct(UUID id, ProductUpdateRequest request);

    void deleteProduct(UUID id);

    void deleteProductByProductId(String productId);
}
