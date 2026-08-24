package org.group1.coffeeshopapi.inventory.repository;

import org.group1.coffeeshopapi.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {
    Page<StockMovement> findByProductId(UUID productId, Pageable pageable);
}