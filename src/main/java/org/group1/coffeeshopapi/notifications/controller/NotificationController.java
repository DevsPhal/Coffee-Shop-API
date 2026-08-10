package org.group1.coffeeshopapi.notifications.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.notifications.dto.request.NotificationCreateRequest;
import org.group1.coffeeshopapi.notifications.dto.response.NotificationResponse;
import org.group1.coffeeshopapi.notifications.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(@Valid @RequestBody NotificationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<NotificationResponse>builder().status(HttpStatus.CREATED.value()).message("Notification created successfully").data(notificationService.create(request)).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder().status(HttpStatus.OK.value()).message("Notifications retrieved successfully").data(notificationService.getAll()).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnread() {
        return ResponseEntity.ok(ApiResponse.<List<NotificationResponse>>builder().status(HttpStatus.OK.value()).message("Unread notifications retrieved successfully").data(notificationService.getUnread()).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        return ResponseEntity.ok(ApiResponse.<Long>builder().status(HttpStatus.OK.value()).message("Unread notification count retrieved successfully").data(notificationService.getUnreadCount()).timeStamp(LocalDateTime.now()).build());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().status(HttpStatus.OK.value()).message("Notification marked as read").timeStamp(LocalDateTime.now()).build());
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(ApiResponse.<Void>builder().status(HttpStatus.OK.value()).message("All notifications marked as read").timeStamp(LocalDateTime.now()).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().status(HttpStatus.OK.value()).message("Notification deleted successfully").timeStamp(LocalDateTime.now()).build());
    }
}