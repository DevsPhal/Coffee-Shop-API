package org.group1.coffeeshopapi.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    private String botToken;
    private String webhookSecret;
    private String webhookBaseUrl;
    private String webhookPath;
    private String apiBaseUrl;
    private long linkCodeTtlSeconds = 300;

    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getWebhookBaseUrl() { return webhookBaseUrl; }
    public void setWebhookBaseUrl(String webhookBaseUrl) { this.webhookBaseUrl = webhookBaseUrl; }

    public String getWebhookPath() { return webhookPath; }
    public void setWebhookPath(String webhookPath) { this.webhookPath = webhookPath; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public long getLinkCodeTtlSeconds() { return linkCodeTtlSeconds; }
    public void setLinkCodeTtlSeconds(long linkCodeTtlSeconds) { this.linkCodeTtlSeconds = linkCodeTtlSeconds; }

    public String fullApiUrl(String method) {
        return apiBaseUrl + botToken + "/" + method;
    }

    public String fullWebhookUrl() {
        return webhookBaseUrl + webhookPath;
    }
}
