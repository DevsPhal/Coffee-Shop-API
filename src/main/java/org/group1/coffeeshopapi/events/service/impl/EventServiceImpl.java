package org.group1.coffeeshopapi.events.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.group1.coffeeshopapi.common.enums.EventStatus;
import org.group1.coffeeshopapi.common.enums.EventType;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.events.dto.request.EventRequest;
import org.group1.coffeeshopapi.events.dto.response.EventResponse;
import org.group1.coffeeshopapi.events.entity.Event;
import org.group1.coffeeshopapi.events.mapper.EventMapper;
import org.group1.coffeeshopapi.events.repository.EventRepository;
import org.group1.coffeeshopapi.events.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    @Override
    public EventResponse createEvent(EventRequest request) {
        Event event = eventMapper.toEntity(request);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        return eventMapper.toResponse(findById(id));
    }

    @Override
    public EventResponse updateEvent(Long id, EventRequest request) {
        Event event = findById(id);
        eventMapper.updateEntity(request, event);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event not found: " + id);
        }
        eventRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        return eventMapper.toResponses(eventRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        return eventMapper.toResponses(eventRepository.findByDateGreaterThanEqualOrderByDateAsc(LocalDate.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByStatus(EventStatus status) {
        return eventMapper.toResponses(eventRepository.findByStatusOrderByDateAsc(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByType(EventType type) {
        return eventMapper.toResponses(eventRepository.findByTypeOrderByDateAsc(type));
    }

    private Event findById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
    }
}
