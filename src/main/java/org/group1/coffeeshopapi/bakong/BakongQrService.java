package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;

public interface BakongQrService {

    /**
     * @param amount   the order total, always in USD (how prices are stored system-wide).
     * @param currency currency to encode the QR in. Null falls back to the configured default
     *                 ({@code bakong.currency}). When {@link Currency#KHR}, {@code amount} is
     *                 converted using {@code bakong.khr-per-usd-rate} before being encoded — the
     *                 resulting {@link BakongQrResult#amount()} reflects what was actually put in
     *                 the QR, not the original USD amount.
     */
    BakongQrResult generateQr(BigDecimal amount, String billNumber, Currency currency);
}
