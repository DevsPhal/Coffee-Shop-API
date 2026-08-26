package org.group1.coffeeshopapi.cart.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.user.entity.Customer;

import java.util.ArrayList;
import java.util.List;

// One active cart per customer. Line items carry no price snapshot — prices are computed live
// from the product (via getFinalPrice) until checkout, where an Order/OrderItem takes the snapshot.
@Getter
@Setter
@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(name = "uk_carts_customer_id", columnNames = "customer_id"))
public class Cart extends BaseEntity {

    // Always a real Customer — unlike Order.handledBy/StockMovement.performedBy and similar audit
    // ids elsewhere, this endpoint is customer-only (see CartController/SecurityConfig) and never
    // reachable by the row-less Super Admin, so a genuine relation is safe here.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }
}
