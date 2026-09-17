package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.event.entity.Event;

public interface TelegramEventService {

    // Sends every upcoming event to the chat, soonest first.
    void sendUpcomingEvents(Long chatId);

    // Broadcasts a new event to every linked customer.
    void announceNewEvent(Event event);

    // Reminds every linked customer, once, about events starting within 24h.
    void sendStartingSoonReminders();
}
