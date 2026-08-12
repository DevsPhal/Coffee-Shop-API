package org.group1.coffeeshopapi;

import org.group1.coffeeshopapi.common.config.TelegramBotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableConfigurationProperties(TelegramBotConfig.class)
public class CoffeeShopApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoffeeShopApiApplication.class, args);
    }
}
