package org.group1.coffeeshopapi.order.service;

import org.group1.coffeeshopapi.common.enums.ReceiptLanguage;

import java.util.UUID;

public interface ReceiptService {

    byte[] generateReceiptPdf(UUID orderId);

    byte[] generateInvoicePdf(UUID orderId, ReceiptLanguage language);
}
