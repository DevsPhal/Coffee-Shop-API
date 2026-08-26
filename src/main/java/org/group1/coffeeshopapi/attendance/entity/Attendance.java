package org.group1.coffeeshopapi.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.entity.BaseEntity;

import java.time.LocalDateTime;

// One row per barista work shift: a check-in, and (once checked out) the matching check-out and
// the minutes worked between them. checkOutAt == null means the shift is still open — a barista
// can only have one open shift at a time, enforced in AttendanceServiceImpl rather than a DB
// constraint. See AttendanceAuditLog for the audit trail of who checked in/out or corrected this.
@Getter
@Setter
@Entity
@Table(name = "attendance_records")
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "barista_id", nullable = false)
    private Barista barista;

    @Column(nullable = false)
    private LocalDateTime checkInAt;

    @Column
    private LocalDateTime checkOutAt;

    @Column
    private Long workedMinutes;

    // Optional free-text note — e.g. an admin's reason when backfilling a missed punch or
    // correcting a shift's times.
    @Column
    private String note;
}
