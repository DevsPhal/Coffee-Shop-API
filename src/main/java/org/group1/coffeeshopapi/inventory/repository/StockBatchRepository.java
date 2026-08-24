package org.group1.coffeeshopapi.inventory.repository;

import org.group1.coffeeshopapi.inventory.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface StockBatchRepository extends JpaRepository<StockBatch, UUID> {

    // FIFO: oldest receipt consumed first.
    List<StockBatch> findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
            UUID productId, BigDecimal minRemainingQuantity);

    // LIFO: newest receipt consumed first.
    List<StockBatch> findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtDesc(
            UUID productId, BigDecimal minRemainingQuantity);
}