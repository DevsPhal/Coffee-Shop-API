package org.group1.coffeeshopapi.bakong.impl;

import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.IndividualInfo;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class BakongQrServiceImpl implements BakongQrService {

    private final BakongProperties bakongProperties;

    @Override
    public BakongQrResult generateQr(BigDecimal amount, String billNumber) {
        if (!bakongProperties.isConfigured()) {
            throw new InvalidOperationException("Bakong payment is not configured");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidOperationException("Amount must be greater than zero");
        }

        IndividualInfo individualInfo = new IndividualInfo();
        individualInfo.setBakongAccountId(bakongProperties.getAccountId());
        individualInfo.setAccountInformation(blankToNull(bakongProperties.getAccountInformation()));
        individualInfo.setAcquiringBank(blankToNull(bakongProperties.getAcquiringBank()));
        individualInfo.setCurrency(toCurrency(bakongProperties.getCurrency()));
        individualInfo.setAmount(amount.doubleValue());
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

        return new BakongQrResult(response.getData().getQr(), response.getData().getMd5());
    }

    private KHQRCurrency toCurrency(String currency) {
        try {
            return currency == null || currency.isBlank() ? KHQRCurrency.USD : KHQRCurrency.valueOf(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOperationException("Unsupported currency for Bakong KHQR: " + currency);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
