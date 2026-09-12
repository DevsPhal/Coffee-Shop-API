package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Config for generating Individual Bakong KHQR codes and checking payment status against the
 * Bakong Open API. {@code token} is the access token NBC issues to a registered {@code email},
 * and it is only valid for about 90 days. Keeping the {@code email} alongside it lets
 * {@code BakongTokenService} call {@code /v1/renew_token} and recover on its own the first time
 * the API answers 401, instead of every payment confirmation quietly failing until someone
 * notices and pastes in a new token by hand.
 * <p>
 * {@code currency} is only the fallback used when a QR is requested without an explicit currency.
 * Bakong KHQR itself supports both {@link Currency#USD} and {@link Currency#KHR} per transaction,
 * so callers may pass either at generation time.
 * <p>
 * All prices in this system are stored in USD. {@code khrPerUsdRate} (env var
 * {@code BAKONG_KHR_PER_USD_RATE}) is only the seed/fallback USD→KHR rate used until an admin
 * sets a live override — see {@code BakongExchangeRateService}, which is what
 * {@code BakongQrService} actually reads at QR-generation time. KHR has no minor unit, so the
 * converted amount is always rounded to a whole number.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "bakong")
public class BakongProperties {
    private String baseUrl;
    private String token;
    /** The address the token was registered to; required only for automatic renewal. */
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
