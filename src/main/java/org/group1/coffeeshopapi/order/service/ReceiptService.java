package org.group1.coffeeshopapi.order.service;

import java.util.UUID;

public interface ReceiptService {

    // Renders a completed order as a printable PDF receipt. This is the only receipt a walk-in
    // sale (no linked customer account, so no Telegram invoice — see OrderServiceImpl.complete)
    // ever gets, but it works for any completed order, staff- or customer-placed alike.
    byte[] generateReceiptPdf(UUID orderId);
}
