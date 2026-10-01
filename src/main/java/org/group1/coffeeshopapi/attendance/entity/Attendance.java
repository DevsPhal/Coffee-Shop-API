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

    @Column
    private String note;
}
