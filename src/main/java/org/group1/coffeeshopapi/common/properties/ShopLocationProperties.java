package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

// The shop's own coordinates, used to compute delivery distance for fee evaluation.
// Optional — distance is just omitted until this is configured.
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "shop.location")
public class ShopLocationProperties {
    private BigDecimal latitude;
    private BigDecimal longitude;

    public boolean isConfigured() {
        return latitude != null && longitude != null;
    }
}
