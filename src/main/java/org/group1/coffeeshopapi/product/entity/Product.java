package org.group1.coffeeshopapi.product.entity;

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
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column
    private String imageUrl;

    @Column(nullable = false, unique = true)
    private String sku;

    // Unit of measure for stock quantities, e.g. "kg", "L", "pcs".
    @Column(nullable = false)
    private String unit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    // Discount is optional and admin-set; null discountType means "no discount configured".
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DiscountType discountType;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column
    private LocalDateTime discountStartAt;

    @Column
    private LocalDateTime discountEndAt;

    public boolean isDiscountActive(LocalDateTime at) {
        if (discountType == null || discountValue == null) {
            return false;
        }
        if (discountStartAt != null && at.isBefore(discountStartAt)) {
            return false;
        }
        return discountEndAt == null || !at.isAfter(discountEndAt);
    }

    public BigDecimal getFinalPrice(LocalDateTime at) {
        if (!isDiscountActive(at)) {
            return price;
        }
        BigDecimal discounted = switch (discountType) {
            case PERCENTAGE -> price.subtract(price.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100)));
            case FIXED -> price.subtract(discountValue);
        };
        return discounted.max(BigDecimal.ZERO);
    }
}
