package org.group1.coffeeshopapi.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.product.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

// Audit trail entry for a single stock-in or stock-cut action. This is an internal/staff-only
// process — the customer is never an attributable actor here, even for a self-service order that
// completes with no staff involved (see OrderServiceImpl.confirmBakong, which attributes that
// case to the Super Admin's id as the system-level actor instead).
@Getter
@Setter
@Entity
@Table(name = "stock_movements")
public class StockMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    // Only set for STOCK_OUT — which cost-flow assumption chose the batches consumed.
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private StockStrategy strategy;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column
    private String note;

    @Column(nullable = false)
    private UUID performedBy;
}
