package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class OtpProperties {
    private boolean logOtp = false;
    private int otpExpiryMinutes = 5;
    private int otpMaxAttempts = 5;
    private int otpResendCooldownSeconds = 60;
}