package org.group1.coffeeshopapi.table.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.realtime.ResourceChangeEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "ix_reservations_table_status", columnList = "table_id, status"),
        @Index(name = "ix_reservations_reserved_for", columnList = "reservedFor")
})
@EntityListeners(ResourceChangeEntityListener.class)
public class Reservation extends BaseEntity {

    // Null only once the table has been deleted; tableNumber keeps what was booked.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private DiningTable diningTable;

    @Column(nullable = false, length = 20)
    private String tableNumber;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(length = 30)
    private String customerPhone;

    @Column(nullable = false)
    private Integer partySize;

    @Column(nullable = false)
    private LocalDateTime reservedFor;

    // reservedFor plus the booking length. No other active booking on the table may overlap [reservedFor, endsAt).
    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.BOOKED;

    @Column(length = 500)
    private String note;

    private UUID createdBy;

    private LocalDateTime seatedAt;

    // When it was completed, cancelled or marked as a no-show.
    private LocalDateTime closedAt;

    @Column(length = 300)
    private String closeReason;

    public void assignTable(DiningTable table) {
        this.diningTable = table;
        this.tableNumber = table.getTableNumber();
    }

    public String currentTableNumber() {
        return diningTable != null ? diningTable.getTableNumber() : tableNumber;
    }
}
