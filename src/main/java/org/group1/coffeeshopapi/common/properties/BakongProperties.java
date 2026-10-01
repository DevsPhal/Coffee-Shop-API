package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bakong")
public class BakongProperties {
    private String baseUrl;
    private String token;
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
    private String appIconUrl;
    private String deeplinkCallbackUrl;

    public boolean isConfigured() {
        return notBlank(token) && notBlank(accountId) && notBlank(merchantName);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
