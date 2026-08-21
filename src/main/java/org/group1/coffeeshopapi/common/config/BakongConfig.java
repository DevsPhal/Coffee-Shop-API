package org.group1.coffeeshopapi.common.config;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class BakongConfig {

    private final BakongProperties bakongProperties;

    @Bean
    public RestClient bakongRestClient() {
        return RestClient.builder()
                .baseUrl(bakongProperties.getBaseUrl())
                .build();
    }
}
