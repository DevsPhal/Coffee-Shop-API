package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// Backs app.cors.allowed-origins (see application.yml) — the browser origins allowed to call this
// API. Binds a comma-separated CORS_ALLOWED_ORIGINS env var (Spring's relaxed binding splits a
// single delimited string into a List for a Collection-typed property) just as easily as a real
// YAML list.
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {
    private List<String> allowedOrigins = List.of();
}
