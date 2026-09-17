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

// One row per order-handling action (created, cash collected, cancelled, etc.), attributed to
// whoever performed it — a full history, since the order itself only remembers the latest actor.
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
