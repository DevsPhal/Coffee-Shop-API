package org.group1.coffeeshopapi.events.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.enums.EventStatus;
import org.group1.coffeeshopapi.common.enums.EventType;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.events.dto.request.EventRequest;
import org.group1.coffeeshopapi.events.dto.response.EventResponse;
import org.group1.coffeeshopapi.events.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<EventResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Event created successfully")
                .data(eventService.createEvent(request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvents() {
        return ResponseEntity.ok(ApiResponse.<List<EventResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Events retrieved successfully")
                .data(eventService.getAllEvents())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents() {
        return ResponseEntity.ok(ApiResponse.<List<EventResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Upcoming events retrieved successfully")
                .data(eventService.getUpcomingEvents())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEventsByStatus(@PathVariable EventStatus status) {
        return ResponseEntity.ok(ApiResponse.<List<EventResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Events retrieved successfully")
                .data(eventService.getEventsByStatus(status))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEventsByType(@PathVariable EventType type) {
        return ResponseEntity.ok(ApiResponse.<List<EventResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Events retrieved successfully")
                .data(eventService.getEventsByType(type))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Event retrieved successfully")
                .data(eventService.getEventById(id))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        return ResponseEntity.ok(ApiResponse.<EventResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Event updated successfully")
                .data(eventService.updateEvent(id, request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Event deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
