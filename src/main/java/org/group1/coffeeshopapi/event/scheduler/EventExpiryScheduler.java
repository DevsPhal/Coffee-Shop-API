package org.group1.coffeeshopapi.event.scheduler;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.event.service.EventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventExpiryScheduler {

    private final EventService eventService;

    // Every 15 minutes is frequent enough that an event's status never looks stale for long,
    // and cheap enough (one query, usually returning nothing) to not matter as overhead.
    // initialDelay keeps this from firing before the app has fully started up.
    @Scheduled(initialDelay = 60_000, fixedRate = 900_000)
    public void expireEndedEvents() {
        eventService.expireEndedEvents();
    }
}
