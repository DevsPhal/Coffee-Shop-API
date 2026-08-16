package org.group1.coffeeshopapi.telegram.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TelegramReceiptService {

    private final TelegramLinkService linkService;
    private final TelegramApiClient apiClient;

    public TelegramReceiptService(TelegramLinkService linkService, TelegramApiClient apiClient) {
        this.linkService = linkService;
        this.apiClient = apiClient;
    }

    /** Returns false if the customer never linked Telegram — caller should fall back to QR/download. */
    public boolean sendReceipt(UUID customerId, byte[] pdfBytes, String orderNumber) {
        Optional<Long> chatId = linkService.findChatIdForCustomer(customerId);
        if (chatId.isEmpty()) {
            return false;
        }
        apiClient.sendDocument(chatId.get(), pdfBytes, "receipt-" + orderNumber + ".pdf",
                "🧾 Receipt for order #" + orderNumber + " — thanks for visiting 590st Cafe!");
        return true;
    }
}