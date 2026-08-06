package org.group1.coffeeshopapi.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class EmailConfig {
    @Value("${MAIL_USERNAME}")
    private String shopEmail;
}
