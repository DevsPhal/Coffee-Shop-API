package org.group1.coffeeshopapi.event.entity;

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
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Admin-created promotional/announcement entry, e.g. a seasonal sale window or in-store event.
@Getter
@Setter
@Entity
@Table(name = "events")
public class Event extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column
    private String imageUrl;

    // GPS pin for the event's venue, shown to customers via the Telegram bot's /events command
    // (see TelegramEventServiceImpl) — same optional, given-together-or-not-at-all convention as
    // Order.deliveryLatitude/deliveryLongitude.
    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    // Null for a change made by the Super Admin, which deliberately has no row in "admins" to
    // reference (see CurrentActor.adminRef()).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Admin createdByAdmin;
}