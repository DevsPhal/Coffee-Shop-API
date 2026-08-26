package org.group1.coffeeshopapi.telegram.command;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventsCommand implements TelegramCommand {
    private final TelegramEventService telegramEventService;

    @Override
    public String name() {
        return "/events";
    }

    @Override
    public String execute(TelegramMessage message, String argument) {
        return telegramEventService.buildUpcomingEvents();
    }

    @Override
    public boolean useHtml() {
        return true;
    }
}
