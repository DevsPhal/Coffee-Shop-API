package org.group1.coffeeshopapi.inventory.repository;

import org.group1.coffeeshopapi.inventory.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockBatchRepository extends JpaRepository<StockBatch, UUID> {

    List<StockBatch> findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
            UUID productId, BigDecimal minRemainingQuantity);

    List<StockBatch> findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtDesc(
            UUID productId, BigDecimal minRemainingQuantity);

    Optional<StockBatch> findFirstByProductIdOrderByCreatedAtDesc(UUID productId);

    // What the stock still on the shelf cost, per product: quantity left and its value at batch cost.
    @Query("select b.product.id as productId, sum(b.remainingQuantity) as quantity, "
            + "sum(b.remainingQuantity * b.unitCost) as value "
            + "from StockBatch b where b.remainingQuantity > 0 and b.product.id in :productIds group by b.product.id")
    List<StockValue> valueOnHand(Collection<UUID> productIds);

    @Query("select b.product.id as productId, sum(b.remainingQuantity) as quantity, "
            + "sum(b.remainingQuantity * b.unitCost) as value "
            + "from StockBatch b where b.remainingQuantity > 0 group by b.product.id")
    List<StockValue> valueOnHand();

    interface StockValue {
        UUID getProductId();
        BigDecimal getQuantity();
        BigDecimal getValue();
    }
}
