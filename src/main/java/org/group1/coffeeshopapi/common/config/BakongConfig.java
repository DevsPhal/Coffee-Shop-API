package org.group1.coffeeshopapi.common.config;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class BakongConfig {

    private final BakongProperties bakongProperties;

    @Bean
    public RestClient bakongRestClient() {
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(Duration.ofSeconds(5));
        requests.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
                .requestFactory(requests)
                .baseUrl(bakongProperties.getBaseUrl())
                .build();
    }
}
