package org.group1.coffeeshopapi.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelegramNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final TelegramLinkService linkService;
    private final TelegramApiClient apiClient;

    public TelegramNotificationService(TelegramLinkService linkService, TelegramApiClient apiClient) {
        this.linkService = linkService;
        this.apiClient = apiClient;
    }

    /** Call this right after a login is confirmed successful. Silent no-op if not linked. */
    public void sendLoginAlert(UUID customerId, String deviceOrIp) {
        Optional<Long> chatId = linkService.findChatIdForCustomer(customerId);
        if (chatId.isEmpty()) {
            return; // customer hasn't linked Telegram — nothing to do
        }

        String time = LocalDateTime.now().format(TIME_FMT);
        String text = "🔐 <b>New login</b>\n\n" +
                "Time: " + time + "\n" +
                (deviceOrIp != null ? "From: " + deviceOrIp + "\n" : "") +
                "\nIf this wasn't you, please reset your password immediately.";

        apiClient.sendMessage(chatId.get(), text);
    }
}
