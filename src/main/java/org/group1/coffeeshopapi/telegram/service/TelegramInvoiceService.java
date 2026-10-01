package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.OrderInvoice;

import java.util.UUID;

public interface TelegramInvoiceService {

    void sendInvoice(UUID customerId, OrderInvoice invoice);
}
