package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements TelegramCommand {

    private final TelegramApiClient apiClient;

    public HelpCommand(TelegramApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public String name() { return "help"; }

    @Override
    public void execute(TelegramMessage message, String argument) {
        apiClient.sendMessage(message.getChat().getId(),
                "<b>Available commands</b>\n" +
                        "/start &lt;code&gt; — link your account\n" +
                        "/menu — see today's menu\n" +
                        "/myorders — your last few orders\n" +
                        "/unlink — unlink this chat from your account\n" +
                        "/help — show this message");
    }
}
