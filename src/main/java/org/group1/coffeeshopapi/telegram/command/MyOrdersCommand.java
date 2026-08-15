package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramMessage;
import org.group1.coffeeshopapi.telegram.repository.TelegramLinkRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MyOrdersCommand implements TelegramCommand {

    private final TelegramApiClient apiClient;
    private final TelegramLinkRepository linkRepository;
    private final OrderHistoryProvider orderHistoryProvider;

    public MyOrdersCommand(TelegramApiClient apiClient,
                           TelegramLinkRepository linkRepository,
                           OrderHistoryProvider orderHistoryProvider) {
        this.apiClient = apiClient;
        this.linkRepository = linkRepository;
        this.orderHistoryProvider = orderHistoryProvider;
    }

    @Override
    public String name() { return "myorders"; }

    @Override
    public void execute(TelegramMessage message, String argument) {
        Long chatId = message.getChat().getId();
        Optional<UUID> customerId = linkRepository.findByChatId(chatId).map(l -> l.getCustomerId());

        if (customerId.isEmpty()) {
            apiClient.sendMessage(chatId, "You're not linked yet. Send /start with your code from the website first.");
            return;
        }

        List<String> orders = orderHistoryProvider.recentOrders(customerId.get(), 5);
        StringBuilder sb = new StringBuilder("<b>Your recent orders</b>\n\n");
        if (orders.isEmpty()) {
            sb.append("No orders yet!");
        } else {
            orders.forEach(o -> sb.append("• ").append(o).append("\n"));
        }
        apiClient.sendMessage(chatId, sb.toString());
    }

    /** Implement this bean by delegating to your existing OrderService. */
    public interface OrderHistoryProvider {
        List<String> recentOrders(UUID customerId, int limit);
    }
}
