package org.group1.coffeeshopapi.order.service;

import java.util.UUID;

public interface ReceiptService {

    byte[] generateReceiptPdf(UUID orderId);

    byte[] generateInvoicePdf(UUID orderId);
}
