package org.group1.coffeeshopapi.bakong.impl;

import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BakongQrServiceImpl implements BakongQrService {

    private final BakongProperties bakongProperties;
    private final BakongExchangeRateService exchangeRateService;

    @Override
    public BakongQrResult generateQr(BigDecimal amount, String billNumber, Currency currency) {
        if (!bakongProperties.isConfigured()) {
            throw new InvalidOperationException("Bakong payment is not configured");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidOperationException("Amount must be greater than zero");
        }

        Currency resolvedCurrency = currency != null ? currency : bakongProperties.getCurrency();
        KHQRCurrency khqrCurrency = toKhqrCurrency(resolvedCurrency);
        BigDecimal encodedAmount = resolvedCurrency == Currency.KHR ? toKhr(amount) : amount;

        IndividualInfo individualInfo = new IndividualInfo();
        individualInfo.setBakongAccountId(bakongProperties.getAccountId());
        individualInfo.setAccountInformation(blankToNull(bakongProperties.getAccountInformation()));
        individualInfo.setAcquiringBank(blankToNull(bakongProperties.getAcquiringBank()));
        individualInfo.setCurrency(khqrCurrency);
        individualInfo.setAmount(encodedAmount.doubleValue());
        individualInfo.setMerchantName(bakongProperties.getMerchantName());
        individualInfo.setMerchantCity(bakongProperties.getMerchantCity());
        individualInfo.setBillNumber(billNumber);
        individualInfo.setStoreLabel(bakongProperties.getStoreLabel());
        individualInfo.setTerminalLabel(bakongProperties.getTerminalLabel());
        individualInfo.setPurposeOfTransaction(bakongProperties.getPurposeOfTransaction());
        individualInfo.setMerchantCategoryCode(bakongProperties.getMerchantCategoryCode());
        individualInfo.setExpirationTimestamp(
                System.currentTimeMillis() + Duration.ofMinutes(bakongProperties.getExpirationMinutes()).toMillis());

        KHQRResponse<KHQRData> response = BakongKHQR.generateIndividual(individualInfo);
        if (response.getKHQRStatus() == null || response.getKHQRStatus().getCode() != 0 || response.getData() == null) {
            String message = response.getKHQRStatus() == null
                    ? "Unknown KHQR generation error"
                    : response.getKHQRStatus().getMessage();
            throw new InvalidOperationException("Unable to generate Bakong QR: " + message);
        }

        return new BakongQrResult(response.getData().getQr(), response.getData().getMd5(), resolvedCurrency, encodedAmount);
    }

    /** Amounts are always priced/stored in USD; KHR has no minor unit, so the converted total must be a whole number. */
    private BigDecimal toKhr(BigDecimal usdAmount) {
        BigDecimal rate = exchangeRateService.getCurrentRate();
        if (rate == null || rate.signum() <= 0) {
            throw new InvalidOperationException("Bakong USD-to-KHR exchange rate is not configured");
        }
        return usdAmount.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    private KHQRCurrency toKhqrCurrency(Currency currency) {
        return currency == Currency.KHR ? KHQRCurrency.KHR : KHQRCurrency.USD;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
