package org.group1.coffeeshopapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableJpaAuditing
@EnableAsync
public class CoffeeShopApiApplication {

    // Every bare LocalDate(Time).now() in the codebase, plus BaseEntity's @CreatedDate/
    // @LastModifiedDate auditing, resolves "now" against the JVM's default zone. Fixing it once
    // here — before anything else in the app runs — keeps all of them (report/expense "today"
    // defaults, order.paidAt, createdAt/updatedAt, ...) consistently on the shop's real-world
    // clock (see application.yml's bakong.merchant-city) regardless of what timezone the host
    // OS/container happens to be set to.
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Phnom_Penh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CoffeeShopApiApplication.class, args);
    }
}
