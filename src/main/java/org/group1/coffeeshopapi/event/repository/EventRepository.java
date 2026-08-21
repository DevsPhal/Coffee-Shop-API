package org.group1.coffeeshopapi.event.repository;

import org.group1.coffeeshopapi.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
}