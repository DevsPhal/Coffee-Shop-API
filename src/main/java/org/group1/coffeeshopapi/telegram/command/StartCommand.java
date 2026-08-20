package org.group1.coffeeshopapi.telegram.command;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.ApiException;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {
    private final TelegramLinkService telegramLinkService;

    @Override
    public String name() {
        return "/start";
    }

    @Override
    public String execute(TelegramMessage message, String argument) {
        if (argument == null || argument.isBlank()) {
            return "Welcome! To link your account, generate a code in the app (Profile -> Link Telegram), " +
                    "then send it here as /start <code>.";
        }
        try {
            return telegramLinkService.resolveLinkCode(argument.trim(), message.chat().id());
        } catch (ApiException ex) {
            return ex.getMessage();
        }
    }
}