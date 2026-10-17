package org.group1.coffeeshopapi.telegram.service.impl;

import com.sun.net.httpserver.HttpServer;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException;
import org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException.Reason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class TelegramApiClientImplTest {

    private static final String BOT_TOKEN = "123456:SECRET-bot-token";

    private final TelegramApiClientImpl client = new TelegramApiClientImpl(new TelegramProperties());
    private final List<String> requests = new ArrayList<>();
    private HttpServer fakeTelegram;

    @AfterEach
    void stopFakeTelegram() {
        if (fakeTelegram != null) {
            fakeTelegram.stop(0);
        }
    }

    @Test
    void sendPhotoIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendPhoto(123L, "https://example.test/flyer.png", "<b>Event</b>"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendPhotoIsANoOpWithoutACaptionToo() {
        assertThatCode(() -> client.sendPhoto(123L, "https://example.test/flyer.png", null))
                .doesNotThrowAnyException();
    }

    @Test
    void sendLocationIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendLocation(123L, new BigDecimal("11.5568"), new BigDecimal("104.9282")))
                .doesNotThrowAnyException();
    }

    @Test
    void sendMessageWithButtonsIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendMessageWithButtons(123L, "pick one"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendHtmlMessageWithButtonsIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendHtmlMessageWithButtons(123L, "<b>pick one</b>"))
                .doesNotThrowAnyException();
    }

    @Test
    void answerCallbackQueryIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.answerCallbackQuery("cbq-1", null))
                .doesNotThrowAnyException();
        assertThatCode(() -> client.answerCallbackQuery("cbq-1", "Done!"))
                .doesNotThrowAnyException();
    }

    @Test
    void aMessageTheUserIsWaitingForFailsLoudlyWhenNoBotTokenIsConfigured() {
        assertThatThrownBy(() -> client.sendHtmlMessageOrThrow(123L, "<b>123456</b>"))
                .isInstanceOf(TelegramDeliveryException.class)
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.NOT_CONFIGURED));
    }

    @Test
    void aDeliveredMessageGoesToSendMessageAsHtml() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(200, "{\"ok\":true,\"result\":{}}"));

        assertThatCode(() -> live.sendHtmlMessageOrThrow(555L, "<b>123456</b>")).doesNotThrowAnyException();

        assertThat(requests).singleElement().satisfies(request -> assertThat(request)
                .startsWith("/bot" + BOT_TOKEN + "/sendMessage ")
                .contains("\"chat_id\":555")
                .contains("\"parse_mode\":\"HTML\""));
    }

    @Test
    void aBlockedBotIsReportedAsAnUnreachableChatThatTellsTheUserToPressStart() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(403,
                "{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}"));

        assertThatThrownBy(() -> live.sendHtmlMessageOrThrow(555L, "code"))
                .isInstanceOf(TelegramDeliveryException.class)
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.CHAT_UNREACHABLE))
                .hasMessageContaining("@CafeBot")
                .hasMessageContaining("tap Start");
    }

    @Test
    void anUnknownChatIsReportedAsUnreachable() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(400,
                "{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}"));

        assertThatThrownBy(() -> live.sendHtmlMessageOrThrow(555L, "code"))
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.CHAT_UNREACHABLE));
    }

    @Test
    void aRejectedBotTokenIsReportedAsMissingConfiguration() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(401,
                "{\"ok\":false,\"error_code\":401,\"description\":\"Unauthorized\"}"));

        assertThatThrownBy(() -> live.sendHtmlMessageOrThrow(555L, "code"))
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.NOT_CONFIGURED));
    }

    @Test
    void rateLimitingOrAnOutageAtTelegramIsReportedAsUnavailable() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(429,
                "{\"ok\":false,\"error_code\":429,\"description\":\"Too Many Requests: retry after 5\"}"));

        assertThatThrownBy(() -> live.sendHtmlMessageOrThrow(555L, "code"))
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.UNAVAILABLE));
    }

    @Test
    void aNetworkFailureIsUnavailableAndTheBotTokenNeverReachesTheLogs(CapturedOutput output) throws IOException {
        TelegramApiClientImpl live = clientFor("http://127.0.0.1:" + closedPort());

        assertThatThrownBy(() -> live.sendHtmlMessageOrThrow(555L, "code"))
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.UNAVAILABLE))
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(BOT_TOKEN));
        live.sendHtmlMessage(555L, "receipt");

        assertThat(output.getAll()).contains("<redacted>").doesNotContain(BOT_TOKEN);
    }

    @Test
    void bestEffortMessagesStillSwallowFailuresSoReceiptsNeverBreakAnOrder() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(403,
                "{\"ok\":false,\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}"));

        assertThatCode(() -> live.sendHtmlMessage(555L, "receipt")).doesNotThrowAnyException();
        assertThatCode(() -> live.sendHtmlMessageWithInlineKeyboard(555L, "confirm?",
                List.of(List.of(Map.of("text", "OK", "callback_data", "x"))))).doesNotThrowAnyException();
        assertThat(requests).hasSize(2);
    }

    private String startFakeTelegram(int status, String body) throws IOException {
        fakeTelegram = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeTelegram.createContext("/", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(exchange.getRequestURI().getPath() + " " + requestBody);
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        });
        fakeTelegram.start();
        return "http://127.0.0.1:" + fakeTelegram.getAddress().getPort();
    }

    private static TelegramApiClientImpl clientFor(String apiBaseUrl) {
        TelegramProperties properties = new TelegramProperties();
        properties.setBotToken(BOT_TOKEN);
        properties.setBotUsername("@CafeBot");
        properties.setApiBaseUrl(apiBaseUrl);
        return new TelegramApiClientImpl(properties);
    }

    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static Reason reasonOf(Throwable ex) {
        return ((TelegramDeliveryException) ex).getReason();
    }
}
