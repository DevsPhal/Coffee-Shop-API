package org.group1.coffeeshopapi.events.repository;

import java.time.LocalDate;
import java.util.List;

import org.group1.coffeeshopapi.common.enums.EventStatus;
import org.group1.coffeeshopapi.common.enums.EventType;
import org.group1.coffeeshopapi.events.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatusOrderByDateAsc(EventStatus status);

    List<Event> findByTypeOrderByDateAsc(EventType type);

    List<Event> findByDateGreaterThanEqualOrderByDateAsc(LocalDate date);
}
