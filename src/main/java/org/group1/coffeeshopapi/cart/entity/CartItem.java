package org.group1.coffeeshopapi.cart.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    // Which extras (e.g. Pearl) the customer opted to add — a plain yes/no toggle per extra, not
    // a quantity. Held live (like sizeOption) rather than snapshotted, since CartItem prices are
    // computed live until checkout — see the Cart javadoc and OrderItemExtra for the snapshot.
    // Its own entity/table (CartItemExtra), same as ProductExtra/OrderItemExtra, rather than a bare
    // join table — keeps every "who's linked to this extra" relation modeled the same way.
    @OneToMany(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemExtra> extras = new ArrayList<>();

    // Replaces the whole selection in one go — orphanRemoval clears whatever was there before, one
    // CartItemExtra row per extra chosen. Used by both addItem (a fresh item) and updateItem
    // (replacing an existing one); an empty set clears every extra off this item.
    public void setExtraSelection(Set<Extra> selected) {
        extras.clear();
        for (Extra extra : selected) {
            CartItemExtra link = new CartItemExtra();
            link.setCartItem(this);
            link.setExtra(extra);
            extras.add(link);
        }
    }

    // Reads the selection back as plain Extra entities — what pricing, the same-line-item
    // comparison, and checkout's forward-to-order-item all actually need, without every caller
    // re-deriving it from the CartItemExtra rows themselves.
    public Set<Extra> getSelectedExtras() {
        return extras.stream().map(CartItemExtra::getExtra).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
