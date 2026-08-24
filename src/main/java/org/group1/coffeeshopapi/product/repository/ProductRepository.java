package org.group1.coffeeshopapi.product.repository;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);
    boolean existsByCategoryId(UUID categoryId);
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByStatus(Status status, Pageable pageable);
    Page<Product> findByCategoryIdAndStatus(UUID categoryId, Status status, Pageable pageable);

    // Small-catalog reads for the Telegram bot menu — no pagination needed for a shop's full menu.
    List<Product> findByStatusOrderByNameAsc(Status status);
    List<Product> findByCategoryIdAndStatusOrderByNameAsc(UUID categoryId, Status status);
}
