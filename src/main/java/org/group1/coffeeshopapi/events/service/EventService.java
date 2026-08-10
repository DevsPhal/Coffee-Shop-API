package org.group1.coffeeshopapi.events.service;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.enums.EventStatus;
import org.group1.coffeeshopapi.common.enums.EventType;
import org.group1.coffeeshopapi.events.dto.request.EventRequest;
import org.group1.coffeeshopapi.events.dto.response.EventResponse;

public interface EventService {
    EventResponse createEvent(EventRequest request);
    EventResponse getEventById(UUID id);
    EventResponse updateEvent(UUID id, EventRequest request);
    void deleteEvent(UUID id);
    List<EventResponse> getAllEvents();
    List<EventResponse> getUpcomingEvents();
    List<EventResponse> getEventsByStatus(EventStatus status);
    List<EventResponse> getEventsByType(EventType type);
}
