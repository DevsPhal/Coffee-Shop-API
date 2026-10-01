package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;

public interface BakongQrService {

    BakongQrResult generateQr(BigDecimal amount, String billNumber, Currency currency);
}
