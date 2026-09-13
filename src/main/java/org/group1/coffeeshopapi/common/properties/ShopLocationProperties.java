package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * The shop's own pinned coordinates (env vars {@code SHOP_LATITUDE}/{@code SHOP_LONGITUDE}) —
 * used only to compute the straight-line distance to a delivery order's pinned drop-off point
 * (see {@code GeoUtil}), so an admin/barista evaluating a delivery fee has a number to work from
 * instead of eyeballing two coordinates. Optional: distance is simply omitted from the order
 * response until this is configured.
 */
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
