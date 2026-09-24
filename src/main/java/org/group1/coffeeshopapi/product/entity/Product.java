package org.group1.coffeeshopapi.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.realtime.ResourceChangeEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@EntityListeners(ResourceChangeEntityListener.class)
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    // Khmer translation of the name, shown alongside the English name — optional.
    @Column
    private String nameKh;

    @Column
    private String description;

    @Column
    private String imageUrl;

    @Column(nullable = false, unique = true)
    private String sku;

    // Unit a stock batch is bought/counted in, e.g. a PACK or CARTON of the sell unit below.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockUnit stockUnit;

    // Unit a single sale is rung up in, e.g. a CUP or PLATE — what the customer actually orders.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellUnit sellUnit;

    // How many sell units one stock unit yields, e.g. a CARTON of 24 CANs -> 24.
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal unitsPerStock = BigDecimal.ONE;

    // A product has no price of its own — every price comes from one of its variants.

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

    // Which admin created or last modified this product — null if it was the Super Admin.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Admin createdByAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Admin updatedByAdmin;

    public boolean isDiscountActive(LocalDateTime at) {
        if (discountType == null || discountValue == null) {
            return false;
        }
        if (discountStartAt != null && at.isBefore(discountStartAt)) {
            return false;
        }
        return discountEndAt == null || !at.isAfter(discountEndAt);
    }

    // Rounded to cents, so line totals, the order total and the Bakong QR amount all match what
    // the database stores (scale 2).
    public BigDecimal getFinalPrice(BigDecimal basePrice, LocalDateTime at) {
        if (!isDiscountActive(at)) {
            return basePrice.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal discounted = switch (discountType) {
            case PERCENTAGE -> basePrice.subtract(basePrice.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100)));
            case FIXED -> basePrice.subtract(discountValue);
        };
        return discounted.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    // Stock is counted in stock units (e.g. CARTONs), sales in sell units (e.g. CANs), so selling
    // 1 CAN of a 24-CAN carton uses 1/24 of a carton. Stock is stored to 3 decimals.
    public BigDecimal toStockQuantity(int sellQuantity) {
        BigDecimal perStock = unitsPerStock != null && unitsPerStock.signum() > 0 ? unitsPerStock : BigDecimal.ONE;
        return BigDecimal.valueOf(sellQuantity).divide(perStock, 3, RoundingMode.HALF_UP)
                .max(new BigDecimal("0.001"));
    }

    // Sellable only while both the product and its category are active.
    public boolean isAvailableForSale() {
        return status == Status.ACTIVE && category != null && category.getStatus() == Status.ACTIVE;
    }
}
