package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.event.entity.Event;

public interface TelegramEventService {

    // Backs /events: every not-yet-ended event, soonest first.
    String buildUpcomingEvents();

    // Broadcasts a new event to every linked customer, the moment an admin creates it.
    void announceNewEvent(Event event);

    // Scans for events starting within the next 24h and reminds every linked customer, once per
    // event. Meant to be called on a recurring schedule (see TelegramEventReminderScheduler).
    void sendStartingSoonReminders();
}
