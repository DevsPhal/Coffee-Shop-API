package org.group1.coffeeshopapi.product.repository;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductSizeOptionRepository extends JpaRepository<ProductSizeOption, UUID> {
    List<ProductSizeOption> findByProductIdOrderBySortOrderAscNameAsc(UUID productId);
    List<ProductSizeOption> findByProductIdAndStatusOrderBySortOrderAscNameAsc(UUID productId, Status status);

    // Batches the customer-facing menu list's size options across a whole page of products.
    List<ProductSizeOption> findByProductIdInAndStatusOrderBySortOrderAscNameAsc(List<UUID> productIds, Status status);

    Optional<ProductSizeOption> findByIdAndProductId(UUID id, UUID productId);
    boolean existsByProductIdAndNameIgnoreCase(UUID productId, String name);
}
