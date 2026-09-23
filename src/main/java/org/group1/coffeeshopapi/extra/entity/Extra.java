package org.group1.coffeeshopapi.extra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.realtime.ResourceChangeEntityListener;

import java.math.BigDecimal;

// A global add-on (e.g. "Pearl") with its own price, shared across every product it's attached to.
@Getter
@Setter
@Entity
@EntityListeners(ResourceChangeEntityListener.class)
@Table(name = "extras")
public class Extra extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    // Flat amount added to a line's unit price when the customer opts into this extra.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    // Null means not stock-tracked (always available). Once set, 0 or less means out of stock.
    @Column(precision = 12, scale = 3)
    private BigDecimal quantityOnHand;

    // Shown next to the add-on choice (e.g. a photo of Pearl) so customers can see what they're adding.
    @Column
    private String imageUrl;
}
