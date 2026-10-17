package org.group1.coffeeshopapi.telegram.service.impl;

import com.sun.net.httpserver.HttpServer;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.TelegramProfilePhoto;
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

    @Test
    void theProfilePhotoLookupPicksTheBiggestAvatarSizedVersion() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(Map.of(
                "/bot" + BOT_TOKEN + "/getUserProfilePhotos", json("{\"ok\":true,\"result\":{\"total_count\":1,"
                        + "\"photos\":[[{\"file_id\":\"s\",\"file_unique_id\":\"us\",\"width\":160},"
                        + "{\"file_id\":\"m\",\"file_unique_id\":\"um\",\"width\":320},"
                        + "{\"file_id\":\"l\",\"file_unique_id\":\"ul\",\"width\":640},"
                        + "{\"file_id\":\"xl\",\"file_unique_id\":\"uxl\",\"width\":1280}]]}}"))));

        assertThat(live.findProfilePhoto(9001L)).contains(new TelegramProfilePhoto("l", "ul"));
        assertThat(requests).singleElement().satisfies(request -> assertThat(request)
                .contains("\"user_id\":9001")
                .contains("\"limit\":1"));
    }

    @Test
    void aUserWithoutAProfilePhotoHasNone() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(Map.of(
                "/bot" + BOT_TOKEN + "/getUserProfilePhotos",
                json("{\"ok\":true,\"result\":{\"total_count\":0,\"photos\":[]}}"))));

        assertThat(live.findProfilePhoto(9001L)).isEmpty();
    }

    @Test
    void aProfilePhotoIsDownloadedThroughTheBotFileEndpoint() throws IOException {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
        TelegramApiClientImpl live = clientFor(startFakeTelegram(Map.of(
                "/bot" + BOT_TOKEN + "/getFile",
                json("{\"ok\":true,\"result\":{\"file_path\":\"photos/file_1.jpg\",\"file_size\":4}}"),
                "/file/bot" + BOT_TOKEN + "/photos/file_1.jpg", new FakeResponse("image/jpeg", jpeg))));

        assertThat(live.downloadFile("l", 1024)).containsExactly(jpeg);
    }

    @Test
    void aProfilePhotoOverTheSizeLimitIsRefusedBeforeItIsDownloaded() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(Map.of(
                "/bot" + BOT_TOKEN + "/getFile",
                json("{\"ok\":true,\"result\":{\"file_path\":\"photos/big.jpg\",\"file_size\":9000000}}"))));

        assertThatThrownBy(() -> live.downloadFile("l", 1024))
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.UNAVAILABLE));
        assertThat(requests).hasSize(1);
    }

    @Test
    void aDownloadThatTurnsOutBiggerThanAdvertisedIsCutOff() throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(Map.of(
                "/bot" + BOT_TOKEN + "/getFile", json("{\"ok\":true,\"result\":{\"file_path\":\"photos/a.jpg\"}}"),
                "/file/bot" + BOT_TOKEN + "/photos/a.jpg", new FakeResponse("image/jpeg", new byte[2048]))));

        assertThatThrownBy(() -> live.downloadFile("l", 1024))
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.UNAVAILABLE));
    }

    @Test
    void aFailedPhotoDownloadNeverLeaksTheBotToken(CapturedOutput output) throws IOException {
        TelegramApiClientImpl live = clientFor(startFakeTelegram(Map.of(
                "/bot" + BOT_TOKEN + "/getFile", json("{\"ok\":true,\"result\":{\"file_path\":\"photos/gone.jpg\"}}"))));

        assertThatThrownBy(() -> live.downloadFile("l", 1024))
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(BOT_TOKEN));
        assertThat(output.getAll()).doesNotContain(BOT_TOKEN);
    }

    @Test
    void photoLookupsFailLoudlyWhenNoBotTokenIsConfigured() {
        assertThatThrownBy(() -> client.findProfilePhoto(9001L))
                .satisfies(ex -> assertThat(reasonOf(ex)).isEqualTo(Reason.NOT_CONFIGURED));
    }

    @Test
    void widgetPhotosAreOnlyFetchedFromTelegramOverHttps() {
        assertThat(TelegramApiClientImpl.telegramHostedImage("https://t.me/i/userpic/320/alice.jpg")).isNotNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage("https://cdn4.telesco.pe/file/alice.jpg")).isNotNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage("http://t.me/i/userpic/320/alice.jpg")).isNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage("https://t.me.attacker.example/a.jpg")).isNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage("https://attacker.example/t.me/a.jpg")).isNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage("https://user@t.me/a.jpg")).isNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage("https://t.me:8443/a.jpg")).isNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage("https://faket.me/a.jpg")).isNull();
        assertThat(TelegramApiClientImpl.telegramHostedImage(null)).isNull();
    }

    @Test
    void aWidgetPhotoUrlOutsideTelegramIsNeverRequested() throws IOException {
        String fake = startFakeTelegram(Map.of("/a.jpg", new FakeResponse("image/jpeg", new byte[]{1})));
        TelegramApiClientImpl live = clientFor(fake);

        assertThatThrownBy(() -> live.downloadWidgetPhoto(fake + "/a.jpg", 1024))
                .isInstanceOf(TelegramDeliveryException.class);
        assertThat(requests).isEmpty();
    }

    private record FakeResponse(String contentType, byte[] body) {
    }

    private static FakeResponse json(String body) {
        return new FakeResponse("application/json", body.getBytes(StandardCharsets.UTF_8));
    }

    private String startFakeTelegram(Map<String, FakeResponse> routes) throws IOException {
        fakeTelegram = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fakeTelegram.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            requests.add(path + " " + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            FakeResponse route = routes.get(path);
            byte[] response = route == null ? "{\"ok\":false}".getBytes(StandardCharsets.UTF_8) : route.body();
            exchange.getResponseHeaders().add("Content-Type", route == null ? "application/json" : route.contentType());
            exchange.sendResponseHeaders(route == null ? 404 : 200, response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        });
        fakeTelegram.start();
        return "http://127.0.0.1:" + fakeTelegram.getAddress().getPort();
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
