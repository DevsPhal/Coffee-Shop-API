package org.group1.coffeeshopapi.cart.entity;

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
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;

@Getter
@Setter
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Variant selection — all optional, since not every product is a customizable drink. Held as
    // a live relation (unlike OrderItem, which snapshots these at checkout) because CartItem
    // prices are computed live from the product/size until checkout — see the Cart javadoc.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_option_id")
    private ProductSizeOption sizeOption;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SugarLevel sugarLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IceLevel iceLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MilkType milkType;
}
