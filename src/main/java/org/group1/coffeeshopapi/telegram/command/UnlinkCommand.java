package org.group1.coffeeshopapi.telegram.command;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.ApiException;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnlinkCommand implements TelegramCommand {
    private final TelegramLinkService telegramLinkService;

    @Override
    public String name() {
        return "/unlink";
    }

    @Override
    public String execute(TelegramMessage message, String argument) {
        try {
            return telegramLinkService.unlink(message.chat().id());
        } catch (ApiException ex) {
            return ex.getMessage();
        }
    }
}