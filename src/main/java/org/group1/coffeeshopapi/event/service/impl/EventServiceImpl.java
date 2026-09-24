package org.group1.coffeeshopapi.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.event.dto.request.CreateEventRequest;
import org.group1.coffeeshopapi.event.dto.request.UpdateEventRequest;
import org.group1.coffeeshopapi.event.dto.response.CustomerEventResponse;
import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.group1.coffeeshopapi.event.entity.Event;
import org.group1.coffeeshopapi.event.mapper.EventMapper;
import org.group1.coffeeshopapi.event.repository.EventRepository;
import org.group1.coffeeshopapi.event.service.EventService;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String IMAGE_FOLDER = "events";

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final FileStorageService fileStorageService;
    private final TelegramEventService telegramEventService;

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request, Admin actorAdmin) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new InvalidOperationException("Event end date must be after the start date");
        }
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new InvalidOperationException("Event latitude and longitude must be given together");
        }

        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setCreatedByAdmin(actorAdmin);
        event = eventRepository.save(event);

        // Broadcasting to Telegram is a side effect, not part of "was the event created" — a bad
        // chat id or a Telegram API hiccup must not turn a successful create into a 500.
        try {
            telegramEventService.announceNewEvent(event);
        } catch (Exception ex) {
            log.error("Failed to announce new event {} on Telegram", event.getId(), ex);
        }

        return toResponse(event);
    }

    @Override
    public EventResponse getById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<EventResponse> list(Pageable pageable) {
        // createdByAdmin loads batched, not N+1, across this page's rows.
        return eventRepository.findAll(pageable).map(eventMapper::toResponse);
    }

    @Override
    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest request) {
        Event event = findById(id);

        if (request.title() != null) {
            event.setTitle(request.title());
        }
        if (request.description() != null) {
            event.setDescription(request.description());
        }
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new InvalidOperationException("Event latitude and longitude must be given together");
        }
        if (request.latitude() != null) {
            event.setLatitude(request.latitude());
            event.setLongitude(request.longitude());
        }
        if (request.startAt() != null) {
            event.setStartAt(request.startAt());
        }
        if (request.endAt() != null) {
            event.setEndAt(request.endAt());
        }
        if (request.status() != null) {
            event.setStatus(request.status());
        }
        if (!event.getEndAt().isAfter(event.getStartAt())) {
            throw new InvalidOperationException("Event end date must be after the start date");
        }

        return toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Event event = findById(id);
        String imageUrl = event.getImageUrl();
        eventRepository.delete(event);
        eventRepository.flush();
        if (imageUrl != null) {
            fileStorageService.delete(imageUrl);
        }
    }

    @Override
    @Transactional
    public EventResponse uploadImage(UUID id, MultipartFile file) {
        Event event = findById(id);
        String previousImageUrl = event.getImageUrl();

        event.setImageUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        event = eventRepository.save(event);

        if (previousImageUrl != null) {
            fileStorageService.delete(previousImageUrl);
        }

        return toResponse(event);
    }

    @Override
    @Transactional
    public EventResponse removeImage(UUID id) {
        Event event = findById(id);
        if (event.getImageUrl() != null) {
            fileStorageService.delete(event.getImageUrl());
            event.setImageUrl(null);
            event = eventRepository.save(event);
        }
        return toResponse(event);
    }

    @Override
    public List<CustomerEventResponse> listUpcoming() {
        return eventRepository.findByStatusAndEndAtAfterOrderByStartAtAsc(Status.ACTIVE, LocalDateTime.now()).stream()
                .map(eventMapper::toCustomerResponse)
                .toList();
    }

    @Override
    @Transactional
    public void expireEndedEvents() {
        List<Event> ended = eventRepository.findByStatusAndEndAtBefore(Status.ACTIVE, LocalDateTime.now());
        for (Event event : ended) {
            event.setStatus(Status.INACTIVE);
        }
        eventRepository.saveAll(ended);
    }

    private EventResponse toResponse(Event event) {
        return eventMapper.toResponse(event);
    }

    private Event findById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }
}
