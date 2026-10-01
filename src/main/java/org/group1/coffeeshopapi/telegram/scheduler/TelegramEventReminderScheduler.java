package org.group1.coffeeshopapi.telegram.scheduler;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramEventReminderScheduler {

    private final TelegramEventService telegramEventService;

    @Scheduled(initialDelay = 60_000, fixedRate = 1_800_000)
    public void remindUpcomingEvents() {
        telegramEventService.sendStartingSoonReminders();
    }
}
