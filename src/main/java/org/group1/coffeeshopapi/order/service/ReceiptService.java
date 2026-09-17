package org.group1.coffeeshopapi.order.service;

import java.util.UUID;

public interface ReceiptService {

    // Renders a finished, paid order (COMPLETED or DELIVERED) as a printable PDF receipt.
    byte[] generateReceiptPdf(UUID orderId);
}
