package org.group1.coffeeshopapi.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.product.entity.Product;

import java.math.BigDecimal;

/**
 * One row per product. {@code quantityOnHand} is a cached running total kept in lockstep with
 * the sum of {@link StockBatch#getRemainingQuantity()} for that product — batches are the
 * source of truth for FIFO/LIFO consumption order, this is the fast-read total.
 */
@Getter
@Setter
@Entity
@Table(name = "inventories")
public class Inventory extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;
}
