package org.group1.coffeeshopapi.inventory.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProduct_Id(UUID productId);

    Optional<Inventory> findByProduct_ProductId(String productCode);

    boolean existsByProduct_Id(UUID productId);

    @Query("SELECT i FROM Inventory i WHERE i.quantityOnHand <= i.lowStockThreshold")
    List<Inventory> findAllLowStock();
}
