package org.group1.coffeeshopapi.events.entity;

import java.time.LocalDate;
import java.util.UUID;
import java.time.LocalDateTime;

import org.group1.coffeeshopapi.common.enums.EventStatus;
import org.group1.coffeeshopapi.common.enums.EventType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 128)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventType type;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "TEXT")
    private String description;

    // @Builder.Default required, else Lombok's builder silently drops this default
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    @Builder.Default
    private EventStatus status = EventStatus.UPCOMING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
