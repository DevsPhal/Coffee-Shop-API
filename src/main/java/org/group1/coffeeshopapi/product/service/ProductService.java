package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.product.dto.request.CreateProductRequest;
import org.group1.coffeeshopapi.product.dto.request.SetProductDiscountRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.group1.coffeeshopapi.product.dto.response.SkuSuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProductService {
    ProductResponse create(CreateProductRequest request, Admin actorAdmin);
    ProductResponse getById(UUID id);

    ProductResponse getOrderableById(UUID id);
    Page<ProductResponse> list(UUID categoryId, Pageable pageable);

    Page<ProductResponse> listActive(UUID categoryId, Pageable pageable);
    ProductResponse update(UUID id, UpdateProductRequest request, Admin actorAdmin);
    void delete(UUID id);

    ProductResponse setDiscount(UUID id, SetProductDiscountRequest request, Admin actorAdmin);
    ProductResponse clearDiscount(UUID id, Admin actorAdmin);

    ProductResponse uploadImage(UUID id, MultipartFile file, Admin actorAdmin);
    ProductResponse removeImage(UUID id, Admin actorAdmin);

    ProductResponse regenerateSku(UUID id, Admin actorAdmin);
    SkuSuggestionResponse suggestSku(UUID categoryId, String name, UUID productId);
}
