package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// Config for generating Bakong KHQR codes and checking payment status via the Bakong Open API.
// The access token expires after ~90 days; email is kept alongside it so the app can renew it
// automatically instead of failing until someone pastes in a new one.
//
// khrPerUsdRate is just the starting exchange rate — an admin can override it later.
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bakong")
public class BakongProperties {
    private String baseUrl;
    private String token;
    // Address the token was registered to; used for automatic renewal.
    private String email;

    private String accountId;
    private String accountInformation;
    private String acquiringBank;
    private String merchantName;
    private String merchantCity;
    private String merchantCategoryCode;
    private String storeLabel;
    private String terminalLabel;
    private String purposeOfTransaction;
    private Currency currency;
    private long expirationMinutes;
    private BigDecimal khrPerUsdRate;

    public boolean isConfigured() {
        return notBlank(token) && notBlank(accountId) && notBlank(merchantName);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
