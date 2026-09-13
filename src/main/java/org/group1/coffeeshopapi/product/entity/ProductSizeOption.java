package org.group1.coffeeshopapi.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;

// A per-product variant choice (e.g. Medium/Large, or a per-piece item), each with its own
// absolute price — unlike sugar level/ice level/milk type (see CartItem/OrderItem), size pricing
// varies by drink so it can't be a single global fixed list. A product has no price of its own;
// every price comes from one of these rows (see ProductPriceResolver).
@Getter
@Setter
@Entity
@Table(name = "product_size_options", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_size_options_product_name", columnNames = {"product_id", "name"})
})
public class ProductSizeOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String name;

    // The absolute price charged when this variant is selected. Never negative.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
}
