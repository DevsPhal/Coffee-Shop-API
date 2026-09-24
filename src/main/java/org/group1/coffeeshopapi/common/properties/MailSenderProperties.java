package org.group1.coffeeshopapi.common.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// Who outgoing email comes from, kept separate from the SMTP login (MAIL_USERNAME) so the
// sender can be the shop's own address even when the login is different.
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "mail.sender")
public class MailSenderProperties {
    private static final String DEFAULT_ADDRESS = "otp@590stcafe.shop";

    private String address = DEFAULT_ADDRESS;
    private String name = "590st Cafe";
    // Shown as "Need help? Contact us at …" and used as Reply-To.
    private String supportEmail;

    // A blank env var (e.g. "MAIL_FROM=" left in .env) arrives as "", not as missing — fall back
    // instead of sending with an empty From/Reply-To, which the mail server rejects.
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
