package org.group1.coffeeshopapi.notifications.repository;

import java.util.List;
import java.util.UUID;

import org.group1.coffeeshopapi.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByReadFalse();
    long countByReadFalse();
}
