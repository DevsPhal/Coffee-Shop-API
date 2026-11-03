package org.group1.coffeeshopapi.inventory.repository;

import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>,
        JpaSpecificationExecutor<StockMovement> {
    Page<StockMovement> findByProductId(UUID productId, Pageable pageable);

    boolean existsByOrderIdAndReason(UUID orderId, StockMovementReason reason);

    List<StockMovement> findByOrderIdAndProductIdAndReason(UUID orderId, UUID productId, StockMovementReason reason);

    // Per product, reason and direction over [from, to): what moved and what it was worth.
    @Query("select m.product.id as productId, m.reason as reason, m.type as type, sum(m.quantity) as quantity, "
            + "sum(coalesce(m.totalCost, 0)) as totalCost, count(m) as movements "
            + "from StockMovement m where m.createdAt >= :from and m.createdAt < :to "
            + "group by m.product.id, m.reason, m.type")
    List<MovementTotals> totalsBetween(LocalDateTime from, LocalDateTime to);

    @Query("select m.wasteReason as wasteReason, sum(coalesce(m.totalCost, 0)) as totalCost from StockMovement m "
            + "where m.reason = org.group1.coffeeshopapi.common.enums.StockMovementReason.WASTE "
            + "and m.createdAt >= :from and m.createdAt < :to group by m.wasteReason")
    List<WasteTotals> wasteBetween(LocalDateTime from, LocalDateTime to);

    interface WasteTotals {
        WasteReason getWasteReason();
        BigDecimal getTotalCost();
    }

    interface MovementTotals {
        UUID getProductId();
        StockMovementReason getReason();
        StockMovementType getType();
        BigDecimal getQuantity();
        BigDecimal getTotalCost();
        Long getMovements();
    }
}
