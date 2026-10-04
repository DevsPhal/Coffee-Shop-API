package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface BakongQrService {

    /** The QR stops being payable at {@code expiresAt}; bank apps refuse it afterwards. */
    BakongQrResult generateQr(BigDecimal amount, String billNumber, Currency currency, LocalDateTime expiresAt);
}
