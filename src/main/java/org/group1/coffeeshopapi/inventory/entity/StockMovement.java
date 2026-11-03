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
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.product.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private StockStrategy strategy;

    // Base units (the product's sell unit), like everything else in stock.
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StockMovementReason reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private WasteReason wasteReason;

    // The order a SALE or RETURN belongs to.
    @Column
    private UUID orderId;

    // What the stock moved was worth: FIFO batch cost for stock out, purchase cost for stock in.
    @Column(precision = 14, scale = 4)
    private BigDecimal totalCost;

    // Quantity on hand right after this movement.
    @Column(precision = 12, scale = 3)
    private BigDecimal balanceAfter;

    @Column
    private String note;

    @Column(nullable = false)
    private UUID performedBy;
}
