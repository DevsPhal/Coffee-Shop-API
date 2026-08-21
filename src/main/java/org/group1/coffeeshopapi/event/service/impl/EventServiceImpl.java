package org.group1.coffeeshopapi.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.event.dto.request.CreateEventRequest;
import org.group1.coffeeshopapi.event.dto.request.UpdateEventRequest;
import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.group1.coffeeshopapi.event.entity.Event;
import org.group1.coffeeshopapi.event.mapper.EventMapper;
import org.group1.coffeeshopapi.event.repository.EventRepository;
import org.group1.coffeeshopapi.event.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String IMAGE_FOLDER = "events";

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request, UUID createdBy) {
        if (!request.endAt().isAfter(request.startAt())) {
            throw new InvalidOperationException("Event end date must be after the start date");
        }

        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setCreatedBy(createdBy);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse getById(UUID id) {
        return eventMapper.toResponse(findById(id));
    }

    @Override
    public Page<EventResponse> list(Pageable pageable) {
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

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        eventRepository.delete(findById(id));
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

        return eventMapper.toResponse(event);
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
        return eventMapper.toResponse(event);
    }

    private Event findById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }
}