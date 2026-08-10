package org.group1.coffeeshopapi.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class EmailConfig {
    @Value("${mail.shop.email:${spring.mail.username:}}")
    private String shopEmail;
}
