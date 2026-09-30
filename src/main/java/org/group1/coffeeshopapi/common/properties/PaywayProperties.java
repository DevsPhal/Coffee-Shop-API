package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// ABA PayWay merchant config, used to open ABA Mobile with the order amount already filled in.
// Merchant id and API key come from ABA after PayWay onboarding (sandbox.payway.com.kh for testing).
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "payway")
public class PaywayProperties {
    private String baseUrl;
    private String merchantId;
    private String apiKey;
    // How long the ABA payment stays open. PayWay allows 3 minutes to 30 days.
    private int lifetimeMinutes;

    public boolean isConfigured() {
        return notBlank(merchantId) && notBlank(apiKey);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
