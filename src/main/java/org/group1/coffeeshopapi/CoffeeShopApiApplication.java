package org.group1.coffeeshopapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableJpaAuditing
public class CoffeeShopApiApplication {

    // Pins the JVM's default timezone to the shop's real-world clock, so every date/time
    // field stays correct no matter what timezone the host is set to.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Phnom_Penh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CoffeeShopApiApplication.class, args);
    }
}
