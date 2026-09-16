package org.group1.coffeeshopapi.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.extra.entity.Extra;

import java.math.BigDecimal;

// One extra (e.g. Pearl) the customer chose to add to an OrderItem — a snapshot, same reasoning as
// OrderItem.productName/unitPrice: extraName/extraPrice freeze what was true at sale time, so a
// later rename/repricing of the Extra doesn't change how a past order displays. The live relation
// is kept anyway (like OrderItem.variant) rather than nulled, so an Extra can't be deleted while
// any order still references it — see GlobalExceptionHandler's DataIntegrityViolationException
// handler.
@Getter
@Setter
@Entity
@Table(name = "order_item_extras")
public class OrderItemExtra extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extra_id", nullable = false)
    private Extra extra;

    @Column(nullable = false)
    private String extraName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal extraPrice;
}
