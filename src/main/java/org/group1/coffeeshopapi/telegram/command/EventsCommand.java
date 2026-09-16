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

    // Sends the event list (and each event's photo/location) directly rather than returning a
    // single reply string — see TelegramEventService.sendUpcomingEvents.
    @Override
    public String execute(TelegramMessage message, String argument) {
        telegramEventService.sendUpcomingEvents(message.chat().id());
        return null;
    }
}
