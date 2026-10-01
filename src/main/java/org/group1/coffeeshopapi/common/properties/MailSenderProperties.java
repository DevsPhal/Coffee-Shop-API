package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mail.sender")
public class MailSenderProperties {
    private static final String DEFAULT_ADDRESS = "otp@590stcafe.shop";

    private String address = DEFAULT_ADDRESS;
    private String name = "590st Cafe";
    private String supportEmail;

    public String getAddress() {
        return isBlank(address) ? DEFAULT_ADDRESS : address.trim();
    }

    public String getSupportEmail() {
        return isBlank(supportEmail) ? getAddress() : supportEmail.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
