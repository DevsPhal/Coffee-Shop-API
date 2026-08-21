package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Config for generating Individual Bakong KHQR codes and checking payment status against the
 * Bakong Open API. {@code token} is a long-lived access token obtained once from NBC's developer
 * portal (registered by email) and stored as a static secret — simpler than the request/renew
 * token flow, and matches how a single-merchant shop is expected to integrate.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bakong")
public class BakongProperties {
    private String baseUrl = "https://api-bakong.nbc.gov.kh";
    private String token;

    private String accountId;
    private String accountInformation;
    private String acquiringBank;
    private String merchantName;
    private String merchantCity = "Phnom Penh";
    private String merchantCategoryCode = "5999";
    private String storeLabel = "Coffee Shop";
    private String terminalLabel = "POS";
    private String purposeOfTransaction = "Payment";
    private String currency = "USD";
    private long expirationMinutes = 15;

    public boolean isConfigured() {
        return notBlank(token) && notBlank(accountId) && notBlank(merchantName);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
