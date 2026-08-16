package org.group1.coffeeshopapi.events.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.enums.EventStatus;
import org.group1.coffeeshopapi.common.enums.EventType;
import org.group1.coffeeshopapi.events.dto.request.EventRequest;
import org.group1.coffeeshopapi.events.dto.response.EventResponse;
import org.group1.coffeeshopapi.events.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse createEvent(@Valid @RequestBody EventRequest request) {
        return eventService.createEvent(request);
    }

    @GetMapping
    public List<EventResponse> getAllEvents() {
        return eventService.getAllEvents();
    }

    @GetMapping("/upcoming")
    public List<EventResponse> getUpcomingEvents() {
        return eventService.getUpcomingEvents();
    }

    @GetMapping("/status/{status}")
    public List<EventResponse> getEventsByStatus(@PathVariable EventStatus status) {
        return eventService.getEventsByStatus(status);
    }

    @GetMapping("/type/{type}")
    public List<EventResponse> getEventsByType(@PathVariable EventType type) {
        return eventService.getEventsByType(type);
    }

    @GetMapping("/{id}")
    public EventResponse getEventById(@PathVariable UUID id) {
        return eventService.getEventById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EventResponse updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteEvent(@PathVariable UUID id) {
        eventService.deleteEvent(id);
    }
}
