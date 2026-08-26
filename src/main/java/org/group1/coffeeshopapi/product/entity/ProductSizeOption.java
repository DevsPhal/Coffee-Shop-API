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

// A per-product size choice (e.g. Small/Medium/Large), each with its own price add-on — unlike
// sugar level/ice level/milk type (see CartItem/OrderItem), size pricing varies by drink so it
// can't be a single global fixed list.
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

    // Added to the product's final price when this size is selected. Can be zero (e.g. the
    // product's base/default size) but never negative.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
}
