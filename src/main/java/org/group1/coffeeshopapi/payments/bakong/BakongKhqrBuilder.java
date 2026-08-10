package org.group1.coffeeshopapi.payments.bakong;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.group1.coffeeshopapi.common.exception.InvalidPaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds an EMVCo-compliant "Bakong KHQR" merchant-presented QR payload (NBC Cambodia).
 * Tag/sub-tag layout follows the publicly documented KHQR structure. Verify against Bakong's
 * sandbox/validator before relying on this for production payments — QR-format bugs are easy
 * to introduce and hard to catch without an actual scan test.
 */
@Component
public class BakongKhqrBuilder {

    private static final Map<String, String> CURRENCY_NUMERIC_CODES = Map.of(
            "USD", "840",
            "KHR", "116");

    @Value("${bakong.account-id}")
    private String bakongAccountId;

    @Value("${bakong.merchant-name}")
    private String merchantName;

    @Value("${bakong.merchant-city}")
    private String merchantCity;

    @Value("${bakong.merchant-category-code}")
    private String merchantCategoryCode;

    public KhqrGenerationResult generate(BigDecimal amount, String currency, String billNumber) {
        String currencyCode = currencyNumericCode(currency);
        String amountStr = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        StringBuilder payload = new StringBuilder();
        payload.append(tlv("00", "01"));
        payload.append(tlv("01", "12"));
        payload.append(tlv("29", tlv("00", "kh.gov.nbc.bakong") + tlv("01", bakongAccountId)));
        payload.append(tlv("52", merchantCategoryCode));
        payload.append(tlv("53", currencyCode));
        payload.append(tlv("54", amountStr));
        payload.append(tlv("58", "KH"));
        payload.append(tlv("59", truncate(merchantName, 25)));
        payload.append(tlv("60", truncate(merchantCity, 15)));
        if (billNumber != null && !billNumber.isBlank()) {
            payload.append(tlv("62", tlv("01", truncate(billNumber, 25))));
        }
        payload.append("6304");

        String crc = crc16Ccitt(payload.toString());
        String qrString = payload + crc;

        return new KhqrGenerationResult(qrString, md5Hex(qrString));
    }

    private String currencyNumericCode(String currency) {
        String code = CURRENCY_NUMERIC_CODES.get(currency == null ? "USD" : currency.toUpperCase());
        if (code == null) {
            throw new InvalidPaymentException("Unsupported currency for Bakong KHQR: " + currency);
        }
        return code;
    }

    private String tlv(String tag, String value) {
        String length = String.format("%02d", value.length());
        return tag + length + value;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String md5Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm unavailable", e);
        }
    }

    /** CRC-16/CCITT-FALSE: poly 0x1021, init 0xFFFF, no reflect — the variant EMVCo QR (tag 63) uses. */
    private String crc16Ccitt(String data) {
        int crc = 0xFFFF;
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }
}
