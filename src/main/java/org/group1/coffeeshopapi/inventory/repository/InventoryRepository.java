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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(UUID productId);

    @Query("select i from Inventory i where i.quantityOnHand <= i.reorderLevel " +
            "order by (i.quantityOnHand - i.reorderLevel) asc")
    Page<Inventory> findLowStock(Pageable pageable);
}
