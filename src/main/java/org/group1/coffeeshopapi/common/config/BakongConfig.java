package org.group1.coffeeshopapi.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class BakongConfig {

    @Value("${bakong.base-url}")
    private String baseUrl;

    @Bean
    public RestTemplate bakongRestTemplate(RestTemplateBuilder builder) {
        return builder
                .rootUri(baseUrl)
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }
}
