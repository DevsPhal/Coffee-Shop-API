package org.group1.coffeeshopapi.telegram.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {

    private static final Logger log = LoggerFactory.getLogger(TelegramConfig.class);

    @Bean
    public WebClient telegramWebClient() {
        return WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Bean
    public CommandLineRunner registerTelegramWebhookAndCommands(WebClient telegramWebClient, TelegramProperties props) {
        return args -> {
            try {
                Map<String, Object> webhookBody = Map.of(
                        "url", props.fullWebhookUrl(),
                        "secret_token", props.getWebhookSecret(),
                        "allowed_updates", new String[]{"message", "callback_query"},
                        "drop_pending_updates", false
                );

                String webhookResp = telegramWebClient.post()
                        .uri(props.fullApiUrl("setWebhook"))
                        .bodyValue(webhookBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                log.info("Telegram setWebhook response: {}", webhookResp);

                Map<String, Object> commandsBody = Map.of(
                        "commands", List.of(
                                Map.of("command", "start", "description", "Link your account"),
                                Map.of("command", "menu", "description", "See today's menu"),
                                Map.of("command", "myorders", "description", "Your recent orders"),
                                Map.of("command", "unlink", "description", "Unlink this chat"),
                                Map.of("command", "help", "description", "Show help")
                        )
                );

                String commandsResp = telegramWebClient.post()
                        .uri(props.fullApiUrl("setMyCommands"))
                        .bodyValue(commandsBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                log.info("Telegram setMyCommands response: {}", commandsResp);

            } catch (Exception e) {
                log.error("Failed to register Telegram webhook/commands — bot will not work correctly", e);
            }
        };
    }
}
