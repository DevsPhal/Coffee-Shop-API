package org.group1.coffeeshopapi.notifications.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.notifications.dto.request.NotificationCreateRequest;
import org.group1.coffeeshopapi.notifications.dto.response.NotificationResponse;
import org.group1.coffeeshopapi.notifications.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public NotificationResponse create(@Valid @RequestBody NotificationCreateRequest request) {
        return notificationService.create(request);
    }

    @GetMapping
    public List<NotificationResponse> getAll() {
        return notificationService.getAll();
    }

    @GetMapping("/unread")
    public List<NotificationResponse> getUnread() {
        return notificationService.getUnread();
    }

    @GetMapping("/unread/count")
    public long unreadCount() {
        return notificationService.getUnreadCount();
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
    }

    @PostMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        notificationService.delete(id);
    }
}
