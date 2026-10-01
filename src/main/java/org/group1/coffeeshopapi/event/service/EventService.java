package org.group1.coffeeshopapi.event.service;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.event.dto.request.CreateEventRequest;
import org.group1.coffeeshopapi.event.dto.request.UpdateEventRequest;
import org.group1.coffeeshopapi.event.dto.response.CustomerEventResponse;
import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface EventService {
    EventResponse create(CreateEventRequest request, Admin actorAdmin);
    EventResponse getById(UUID id);
    Page<EventResponse> list(Pageable pageable);
    EventResponse update(UUID id, UpdateEventRequest request);
    void delete(UUID id);

    EventResponse uploadImage(UUID id, MultipartFile file);
    EventResponse removeImage(UUID id);

    void expireEndedEvents();

    List<CustomerEventResponse> listUpcoming();
}
