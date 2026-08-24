package org.group1.coffeeshopapi.event.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.event.dto.request.CreateEventRequest;
import org.group1.coffeeshopapi.event.dto.request.UpdateEventRequest;
import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.group1.coffeeshopapi.event.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@Tag(name = "Event", description = "Admin only: manage promotional/announcement events")
@SecurityRequirement(name = "bearerAuth")
public class EventController {

    private final EventService eventService;
    private final CurrentActor currentActor;

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse event = eventService.create(request, currentActor.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Event created successfully.", event));
    }

    @GetMapping
    public ApiResponse<PageResponse<EventResponse>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(eventService.list(PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<EventResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, eventService.getById(id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<EventResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateEventRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Event updated successfully.", eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        eventService.delete(id);
        return ApiResponse.of(HttpStatus.OK, "Event deleted successfully.", null);
    }

    @PostMapping(value = "/{id}/image", consumes = "multipart/form-data")
    public ApiResponse<EventResponse> uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.of(HttpStatus.OK, "Event image uploaded successfully.", eventService.uploadImage(id, file));
    }

    @DeleteMapping("/{id}/image")
    public ApiResponse<EventResponse> removeImage(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Event image removed successfully.", eventService.removeImage(id));
    }
}