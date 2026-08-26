package org.group1.coffeeshopapi.order.entity;

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
import org.group1.coffeeshopapi.common.enums.OrderAuditAction;

import java.util.UUID;

// One row per order-handling action (created / cash collected / Bakong confirmed / cancelled),
// each attributed to whichever admin, barista, or customer performed it — see ActorLookupService.
// Together these form the audit trail for "who handled/processed/served this order", which
// Order.handledBy alone can't answer since it's overwritten every time a different staff member
// touches the order (e.g. barista A rings it up, barista B or an admin later collects the cash).
@Getter
@Setter
@Entity
@Table(name = "order_audit_logs")
public class OrderAuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderAuditAction action;

    @Column(nullable = false)
    private UUID actorId;
}
