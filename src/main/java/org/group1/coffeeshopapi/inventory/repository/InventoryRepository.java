package org.group1.coffeeshopapi.inventory.repository;

import jakarta.persistence.LockModeType;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
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
}
