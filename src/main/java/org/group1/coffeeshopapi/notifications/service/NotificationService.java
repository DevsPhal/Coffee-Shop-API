package org.group1.coffeeshopapi.notifications.service;

import java.util.List;

import org.group1.coffeeshopapi.notifications.dto.request.NotificationCreateRequest;
import org.group1.coffeeshopapi.notifications.dto.response.NotificationResponse;

public interface NotificationService {
    NotificationResponse create(NotificationCreateRequest request);
    List<NotificationResponse> getAll();
    List<NotificationResponse> getUnread();
    long getUnreadCount();
    void markRead(Long id);
    void markAllRead();
    void delete(Long id);
}