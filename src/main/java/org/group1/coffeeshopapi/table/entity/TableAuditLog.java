package org.group1.coffeeshopapi.table.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableStatus;

import java.util.UUID;

// Every table status change, who made it and why. Plain ids (no foreign keys) so the history outlives
// a deleted table, order or reservation.
@Getter
@Setter
@Entity
@Table(name = "table_audit_logs", indexes = @Index(name = "ix_table_audit_logs_table", columnList = "tableId, createdAt"))
public class TableAuditLog extends BaseEntity {

    @Column(nullable = false)
    private UUID tableId;

    @Column(nullable = false, length = 20)
    private String tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TableAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TableStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus toStatus;

    @Column(nullable = false)
    private Integer guestCount = 0;

    // Null when the system made the change (a booking hold or an automatic no-show).
    private UUID actorId;

    // An admin bypassed a table rule; reason says why.
    @Column(nullable = false)
    private boolean override;

    @Column(length = 300)
    private String reason;

    private UUID orderId;

    private UUID reservationId;
}
