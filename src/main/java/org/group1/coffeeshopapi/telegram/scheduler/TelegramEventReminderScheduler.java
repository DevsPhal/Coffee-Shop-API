package org.group1.coffeeshopapi.telegram.scheduler;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramEventReminderScheduler {

    private final TelegramEventService telegramEventService;

    // Every 30 minutes is frequent enough that no "starting within 24h" reminder is meaningfully
    // late, and cheap enough (one query, usually returning nothing) to not matter as overhead.
    // initialDelay keeps this from firing before the app has fully started up.
    @Scheduled(initialDelay = 60_000, fixedRate = 1_800_000)
    public void remindUpcomingEvents() {
        telegramEventService.sendStartingSoonReminders();
    }
}
