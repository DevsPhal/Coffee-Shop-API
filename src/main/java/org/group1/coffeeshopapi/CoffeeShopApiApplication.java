package org.group1.coffeeshopapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CoffeeShopApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoffeeShopApiApplication.class, args);
    }
}
