package org.group1.coffeeshopapi.order.service;

import java.util.UUID;

public interface ReceiptService {

    // Renders a finished, paid order (COMPLETED or DELIVERED) as a printable PDF receipt.
    byte[] generateReceiptPdf(UUID orderId);

    // Same PDF, available as soon as the order is paid — no need to wait for it to be
    // prepared/delivered/completed first.
    byte[] generateInvoicePdf(UUID orderId);
}
