package org.group1.coffeeshopapi.inventory.repository;

import jakarta.persistence.LockModeType;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductId(UUID productId);

    // Locks the row for the duration of the stock-in/stock-cut transaction so concurrent
    // requests against the same product serialize instead of racing on quantityOnHand.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(UUID productId);

    // Comparing two columns of the same row isn't expressible as a derived query method, hence
    // the explicit JPQL. Ordered worst-first (most depleted relative to its reorder point) so the
    // most urgent restocks surface at the top of the report.
    @Query("select i from Inventory i where i.quantityOnHand <= i.reorderLevel " +
            "order by (i.quantityOnHand - i.reorderLevel) asc")
    Page<Inventory> findLowStock(Pageable pageable);
}
