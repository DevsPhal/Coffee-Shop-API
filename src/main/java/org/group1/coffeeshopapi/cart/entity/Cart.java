package org.group1.coffeeshopapi.cart.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// One active cart per customer. Line items carry no price snapshot — prices are computed live
// from the product (via getFinalPrice) until checkout, where an Order/OrderItem takes the snapshot.
@Getter
@Setter
@Entity
@Table(name = "carts", uniqueConstraints = @UniqueConstraint(name = "uk_carts_customer_id", columnNames = "customer_id"))
public class Cart extends BaseEntity {

    @Column(nullable = false)
    private UUID customerId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }
}
