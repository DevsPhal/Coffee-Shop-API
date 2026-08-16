package org.group1.coffeeshopapi.payments.dto.request;

import jakarta.validation.constraints.NotNull;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BakongQrRequest {

    private String bakongAccountId;

    private KHQRCurrency currency;

    @NotNull(message = "Amount is required")
    private Double amount;

    private String merchantName;
    private String merchantCity;
    private String merchantId;
    private String acquiringBank;
    private String merchantCategoryCode;
    private String upiAccountInformation;
    private Long expirationTimestamp;
    private String billNumber;
    private String storeLabel;
    private String terminalLabel;
    private String mobileNumber;
    private String purposeOfTransaction;
    private String merchantAlternateLanguagePreference;
    private String merchantNameAlternateLanguage;
    private String merchantCityAlternateLanguage;
}
