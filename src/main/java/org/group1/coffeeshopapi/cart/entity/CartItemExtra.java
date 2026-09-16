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

// One extra (e.g. Pearl) the customer has toggled on for one CartItem — its own table/entity, same
// as ProductExtra (product <-> extra) and OrderItemExtra (order item <-> extra), rather than a bare
// join table with no id of its own: keeps every "who's linked to this extra" relation in this
// feature modeled the same consistent way. Held live (like CartItem.variant) rather than
// snapshotted, since CartItem prices are computed live until checkout — see the Cart javadoc and
// OrderItemExtra for the snapshot taken at that point.
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
