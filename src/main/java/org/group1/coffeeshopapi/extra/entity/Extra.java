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

@Getter
@Setter
@Entity
@EntityListeners(ResourceChangeEntityListener.class)
@Table(name = "extras")
public class Extra extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantityOnHand;

    @Column
    private String imageUrl;
}
