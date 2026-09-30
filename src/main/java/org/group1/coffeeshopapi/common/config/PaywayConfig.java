package org.group1.coffeeshopapi.common.config;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.properties.PaywayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class PaywayConfig {

    private final PaywayProperties paywayProperties;

    @Bean
    public RestClient paywayRestClient() {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(5));
        requests.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder()
                .requestFactory(requests)
                .baseUrl(paywayProperties.getBaseUrl())
                .build();
    }
}
