package org.group1.coffeeshopapi.notifications.service.impl;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.notifications.dto.request.NotificationCreateRequest;
import org.group1.coffeeshopapi.notifications.dto.response.NotificationResponse;
import org.group1.coffeeshopapi.notifications.entity.Notification;
import org.group1.coffeeshopapi.notifications.mapper.NotificationMapper;
import org.group1.coffeeshopapi.notifications.repository.NotificationRepository;
import org.group1.coffeeshopapi.notifications.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    public NotificationResponse create(NotificationCreateRequest request) {
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .read(false)
                .build();
        return notificationMapper.toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread() {
        return notificationRepository.findByReadFalse().stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    @Override
    public void markRead(UUID id) {
        Notification notification = findById(id);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead() {
        notificationRepository.findByReadFalse()
                .forEach(notification -> notification.setRead(true));
    }

    @Override
    public void delete(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        notificationRepository.deleteById(id);
    }

    private Notification findById(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }
}
