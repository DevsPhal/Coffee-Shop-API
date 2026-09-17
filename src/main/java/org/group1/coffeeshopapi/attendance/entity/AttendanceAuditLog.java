package org.group1.coffeeshopapi.attendance.entity;

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
import org.group1.coffeeshopapi.common.enums.AttendanceAuditAction;

import java.util.UUID;

// One row per attendance action (check-in, check-out, admin backfill/correction) — who did it and
// when.
@Getter
@Setter
@Entity
@Table(name = "attendance_audit_logs")
public class AttendanceAuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceAuditAction action;

    @Column(nullable = false)
    private UUID actorId;

    @Column
    private String note;
}
