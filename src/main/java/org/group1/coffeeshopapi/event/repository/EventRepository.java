package org.group1.coffeeshopapi.event.repository;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByStatusAndEndAtAfterOrderByStartAtAsc(Status status, LocalDateTime now);

    List<Event> findByStatusAndStartAtBetween(Status status, LocalDateTime start, LocalDateTime end);

    List<Event> findByStatusAndEndAtBefore(Status status, LocalDateTime now);
}
