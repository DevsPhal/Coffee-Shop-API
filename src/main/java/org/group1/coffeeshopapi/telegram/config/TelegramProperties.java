package org.group1.coffeeshopapi.telegram.config;

import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String botToken;
    private String botUsername;
    private String webhookSecret;
    private String webhookBaseUrl;
    private String webhookPath = SecurityConstants.TELEGRAM_WEBHOOK_PATH;
    private String apiBaseUrl = "https://api.telegram.org";
    private int linkCodeTtlSeconds = 300;
    private boolean autoRegisterWebhook = true;

    public String apiUrl(String method) {
        return apiBaseUrl + "/bot" + botToken + "/" + method;
    }

    public String fullWebhookUrl() {
        return webhookBaseUrl + webhookPath;
    }

    public String deepLink(String code) {
        return "https://t.me/" + botUsername + "?start=" + code;
    }
}