package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.product.dto.request.CreateProductRequest;
import org.group1.coffeeshopapi.product.dto.request.SetProductDiscountRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductImportResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProductService {
    ProductResponse create(CreateProductRequest request);
    ProductResponse getById(UUID id);
    Page<ProductResponse> list(UUID categoryId, Pageable pageable);

    // Customer-facing menu: only products currently on sale.
    Page<ProductResponse> listActive(UUID categoryId, Pageable pageable);
    ProductResponse update(UUID id, UpdateProductRequest request);
    void delete(UUID id);

    ProductResponse setDiscount(UUID id, SetProductDiscountRequest request);
    ProductResponse clearDiscount(UUID id);

    ProductResponse uploadImage(UUID id, MultipartFile file);
    ProductResponse removeImage(UUID id);

    // Bulk-creates products from an .xlsx sheet (columns: name, description, sku, unit, price,
    // category name, reorder level). Rows that fail validation are skipped and reported, valid
    // rows are still committed — see ProductServiceImpl for why this is safe against Postgres's
    // abort-whole-transaction-on-error behavior.
    ProductImportResponse importFromExcel(MultipartFile file);
}
