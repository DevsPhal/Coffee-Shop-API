package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MenuCommand implements TelegramCommand {

    private final TelegramApiClient apiClient;
    private final MenuProvider menuProvider;

    public MenuCommand(TelegramApiClient apiClient, MenuProvider menuProvider) {
        this.apiClient = apiClient;
        this.menuProvider = menuProvider;
    }

    @Override
    public String name() { return "menu"; }

    @Override
    public void execute(TelegramMessage message, String argument) {
        List<String> lines = menuProvider.availableItems();
        StringBuilder sb = new StringBuilder("<b>Today's Menu</b>\n\n");
        if (lines.isEmpty()) {
            sb.append("Nothing published yet — check back soon!");
        } else {
            lines.forEach(l -> sb.append("• ").append(l).append("\n"));
        }
        apiClient.sendMessage(message.getChat().getId(), sb.toString());
    }

    /** Implement this bean by delegating to your existing ProductService. */
    public interface MenuProvider {
        List<String> availableItems();
    }
}
