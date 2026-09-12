package org.group1.coffeeshopapi.extra.repository;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductExtraRepository extends JpaRepository<ProductExtra, UUID> {
    List<ProductExtra> findByProductIdOrderBySortOrderAscId(UUID productId);
    List<ProductExtra> findByProductIdAndStatusOrderBySortOrderAscId(UUID productId, Status status);

    // Batches the customer-facing menu list's extras across a whole page of products.
    List<ProductExtra> findByProductIdInAndStatusOrderBySortOrderAscId(List<UUID> productIds, Status status);

    Optional<ProductExtra> findByIdAndProductId(UUID id, UUID productId);
    Optional<ProductExtra> findByProductIdAndExtraId(UUID productId, UUID extraId);
    boolean existsByProductIdAndExtraId(UUID productId, UUID extraId);

    // Used to validate a set of chosen extraIds all belong to (and are active on) one product —
    // see CartServiceImpl/OrderServiceImpl resolving a cart/order item's extras.
    List<ProductExtra> findByProductIdAndExtraIdInAndStatus(UUID productId, List<UUID> extraIds, Status status);
}
