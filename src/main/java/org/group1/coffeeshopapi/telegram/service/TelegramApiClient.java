package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.outgoing.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class TelegramApiClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramApiClient.class);

    private final WebClient webClient;
    private final TelegramProperties properties;

    public TelegramApiClient(WebClient telegramWebClient, TelegramProperties properties) {
        this.webClient = telegramWebClient;
        this.properties = properties;
    }

    public void sendMessage(Long chatId, String text) {
        webClient.post()
                .uri(properties.fullApiUrl("sendMessage"))
                .bodyValue(new SendMessageRequest(chatId, text))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("sendMessage failed for chatId={}", chatId, e))
                .subscribe();
    }

    public void sendDocument(Long chatId, byte[] pdfBytes, String filename, String caption) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("chat_id", String.valueOf(chatId));
        if (caption != null) {
            builder.part("caption", caption);
        }
        builder.part("document", pdfBytes)
                .filename(filename)
                .contentType(MediaType.APPLICATION_PDF);

        webClient.post()
                .uri(properties.fullApiUrl("sendDocument"))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(e -> log.error("sendDocument failed for chatId={}", chatId, e))
                .subscribe();
    }
}
