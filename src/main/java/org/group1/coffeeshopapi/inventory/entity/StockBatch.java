package org.group1.coffeeshopapi.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.product.entity.Product;

import java.math.BigDecimal;

/**
 * A single stock receipt ("lot") for a product. {@code remainingQuantity} is drawn down as
 * stock is cut; {@code createdAt} (inherited, immutable) is the FIFO/LIFO ordering key — the
 * oldest batch by {@code createdAt} is consumed first under FIFO, the newest first under LIFO.
 */
@Getter
@Setter
@Entity
@Table(name = "stock_batches")
public class StockBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal remainingQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;
}
