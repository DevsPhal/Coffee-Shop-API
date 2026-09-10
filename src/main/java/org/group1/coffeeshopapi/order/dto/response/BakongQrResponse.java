package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code amount} is what the QR actually encodes, already converted for the chosen currency —
 * a KHR code carries whole riel, not the order's USD total. Payment screens should display this
 * rather than converting the total themselves, so the figure shown always matches the figure
 * the customer's wallet will charge.
 * <p>
 * {@code expiresAt} is a zone-less shop-local time, which is fine for display but wrong to count
 * down from on a phone in another timezone. {@code expiresInSeconds} is the same deadline as a
 * duration, so a countdown can be driven from it without knowing either clock's offset.
 */
public record BakongQrResponse(
        UUID orderId,
        String qrString,
        String md5Hash,
        BigDecimal amount,
        Currency currency,
        LocalDateTime expiresAt,
        long expiresInSeconds
) {
}
