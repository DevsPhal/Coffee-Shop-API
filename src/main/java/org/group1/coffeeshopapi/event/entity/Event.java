package org.group1.coffeeshopapi.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(nullable = false)
    private UUID createdBy;
}