package org.group1.coffeeshopapi.event.scheduler;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.event.service.EventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventExpiryScheduler {

    private final EventService eventService;

    // Every 15 minutes; initialDelay avoids firing before the app has fully started.
    @Scheduled(initialDelay = 60_000, fixedRate = 900_000)
    public void expireEndedEvents() {
        eventService.expireEndedEvents();
    }
}
