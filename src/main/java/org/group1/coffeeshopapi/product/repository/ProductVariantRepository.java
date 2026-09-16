package org.group1.coffeeshopapi.product.repository;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProductIdOrderBySortOrderAscNameAsc(UUID productId);
    List<ProductVariant> findByProductIdAndStatusOrderBySortOrderAscNameAsc(UUID productId, Status status);

    // Batches the customer-facing menu list's variants across a whole page of products.
    List<ProductVariant> findByProductIdInAndStatusOrderBySortOrderAscNameAsc(List<UUID> productIds, Status status);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);
    // No more IgnoreCase: name is a closed enum now (see VariantLabel), so there's no casing left
    // to normalize — every valid value is already canonical.
    Optional<ProductVariant> findByProductIdAndName(UUID productId, VariantLabel name);
    boolean existsByProductIdAndName(UUID productId, VariantLabel name);
}
