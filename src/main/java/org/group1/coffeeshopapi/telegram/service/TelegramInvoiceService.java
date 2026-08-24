package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.OrderInvoice;

import java.util.UUID;

public interface TelegramInvoiceService {

    /** No-op if the customer doesn't exist or hasn't linked a Telegram chat — Telegram delivery is best-effort. */
    void sendInvoice(UUID customerId, OrderInvoice invoice);
}
