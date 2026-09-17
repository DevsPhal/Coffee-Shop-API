package org.group1.coffeeshopapi.cart.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.extra.entity.Extra;

// One extra (e.g. Pearl) toggled on for a cart item. A live relation, since cart prices are
// computed live until checkout.
@Getter
@Setter
@Entity
@Table(name = "cart_item_extras", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cart_item_extras_item_extra", columnNames = {"cart_item_id", "extra_id"})
})
public class CartItemExtra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extra_id", nullable = false)
    private Extra extra;
}
