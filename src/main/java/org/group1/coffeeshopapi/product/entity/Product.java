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
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;

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

    // Unit a stock batch is bought/counted in, e.g. a PACK or CARTON of the sell unit below.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockUnit stockUnit;

    // Unit a single sale is rung up in, e.g. a CUP or PLATE — what the customer actually orders.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellUnit sellUnit;

    // How many sell units one stock unit yields, e.g. a CARTON of 24 CANs -> 24. Used to cut
    // inventory by the right amount when an order sells sellUnit-denominated quantities.
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal unitsPerStock = BigDecimal.ONE;

    // A product has no price of its own — every price comes from one of its ProductVariant
    // rows (see ProductPriceResolver). Discount config below still lives here since it applies
    // uniformly to whichever variant prices a given line.

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

    // Which admin created or last modified this product — null both for pre-existing rows (from
    // before this tracking existed) and for a change made by the Super Admin, which deliberately
    // has no row in "admins" to reference (see CurrentActor.adminRef()).
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

    public BigDecimal getFinalPrice(BigDecimal basePrice, LocalDateTime at) {
        if (!isDiscountActive(at)) {
            return basePrice;
        }
        BigDecimal discounted = switch (discountType) {
            case PERCENTAGE -> basePrice.subtract(basePrice.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100)));
            case FIXED -> basePrice.subtract(discountValue);
        };
        return discounted.max(BigDecimal.ZERO);
    }
}
