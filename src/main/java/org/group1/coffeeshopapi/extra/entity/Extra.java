package org.group1.coffeeshopapi.extra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;

// A global add-on catalog entry — e.g. "Pearl" — created once by an admin with its own price, then
// offered on whichever products should carry it (see ProductExtra). Unlike ProductVariant
// (which prices a product's variant and only ever belongs to one product), an Extra is shared
// across every product it's attached to, so its price lives here rather than on the attachment.
@Getter
@Setter
@Entity
@Table(name = "extras")
public class Extra extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    // The flat amount added to a line's unit price when the customer opts into this extra. Never
    // negative.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    // Null means "not stock-tracked" — always treated as available (the behavior every Extra had
    // before this field existed), so adding this doesn't silently hide every pre-existing extra
    // the moment it ships. An admin opts an extra into stock tracking simply by setting a real
    // quantity (see ExtraServiceImpl); from then on 0 (or less) means out of stock. A plain running
    // count, not the FIFO-batch system Inventory uses for products — see ExtraServiceImpl's
    // javadoc for why a topping like this doesn't need that.
    @Column(precision = 12, scale = 3)
    private BigDecimal quantityOnHand;
}
