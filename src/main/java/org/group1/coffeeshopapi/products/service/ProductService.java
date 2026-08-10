package org.group1.coffeeshopapi.products.service;

import java.util.List;

import org.group1.coffeeshopapi.products.dto.request.ProductCreateRequest;
import org.group1.coffeeshopapi.products.dto.request.ProductUpdateRequest;
import org.group1.coffeeshopapi.products.dto.response.ProductResponse;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductByProductId(String productId);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getAllActiveProducts();

    List<ProductResponse> getProductsByCategory(String categoryCode);

    List<ProductResponse> searchProductsByName(String name);

    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    void deleteProduct(Long id);

    void deleteProductByProductId(String productId);
}
