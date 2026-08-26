package org.group1.coffeeshopapi.event.repository;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    // The customer-facing /events list: still-relevant events (hasn't ended yet), soonest first.
    List<Event> findByStatusAndEndAtAfterOrderByStartAtAsc(Status status, LocalDateTime now);

    // Backs the 24h-before reminder scan — events whose startAt falls in the scheduler's lookahead window.
    List<Event> findByStatusAndStartAtBetween(Status status, LocalDateTime start, LocalDateTime end);
}
