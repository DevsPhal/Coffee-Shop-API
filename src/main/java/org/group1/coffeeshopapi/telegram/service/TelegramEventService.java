package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.event.entity.Event;

public interface TelegramEventService {

    void sendUpcomingEvents(Long chatId);

    void announceNewEvent(Event event);

    void sendStartingSoonReminders();
}
