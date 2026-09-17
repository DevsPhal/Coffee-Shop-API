package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;

public interface BakongQrService {

    // amount is always in USD. currency is what to encode the QR in — null falls back to the
    // configured default, and KHR converts the amount before encoding.
    BakongQrResult generateQr(BigDecimal amount, String billNumber, Currency currency);
}
