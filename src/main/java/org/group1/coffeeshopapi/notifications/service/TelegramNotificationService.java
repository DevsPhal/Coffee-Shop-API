package org.group1.coffeeshopapi.notifications.service;

public interface TelegramNotificationService {
    void notifyAdmin(String title, String message);
}
