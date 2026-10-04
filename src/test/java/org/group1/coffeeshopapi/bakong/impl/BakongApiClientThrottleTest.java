package org.group1.coffeeshopapi.bakong.impl;

import org.group1.coffeeshopapi.bakong.BakongTokenService;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BakongApiClientThrottleTest {

    private static final String CHECK_URL = "https://bakong.test/v1/check_transaction_by_md5";
    private static final String NOT_FOUND = """
            {"responseCode":1,"responseMessage":"Transaction could not be found.","errorCode":1,"data":null}""";
    private static final String PAID = """
            {"responseCode":0,"responseMessage":"Success","errorCode":null,
             "data":{"hash":"tx-1","amount":"2.5","currency":"USD"}}""";
    private static final String DAILY_LIMIT = """
            {"responseCode":1,"responseMessage":"Daily request limit of 100 exceeded. Please try again tomorrow.",
             "errorCode":17,"data":null}""";

    private MockRestServiceServer server;
    private MutableClock clock;
    private BakongApiClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bakong.test");
        server = MockRestServiceServer.bindTo(builder).build();

        BakongProperties properties = new BakongProperties();
        properties.setToken("token");
        properties.setAccountId("shop@bank");
        properties.setMerchantName("590st Cafe");

        BakongTokenService tokenService = mock(BakongTokenService.class);
        when(tokenService.currentToken()).thenReturn("token");

        clock = new MutableClock(Instant.parse("2026-10-04T03:00:00Z"));
        client = new BakongApiClientImpl(builder.build(), properties, tokenService, clock);
    }

    @Test
    void repeatedChecksOfAFreshQrReuseTheLastAnswer() {
        server.expect(ExpectedCount.once(), requestTo(CHECK_URL))
                .andRespond(withSuccess(NOT_FOUND, MediaType.APPLICATION_JSON));

        client.checkTransactionByMd5("md5");
        clock.advance(Duration.ofSeconds(4));
        BakongTransactionCheckResult second = client.checkTransactionByMd5("md5");

        assertThat(second.paid()).isFalse();
        assertThat(second.failed()).isFalse();
        server.verify();
    }

    @Test
    void freshQrIsCheckedAgainAfterTheInterval() {
        server.expect(ExpectedCount.once(), requestTo(CHECK_URL))
                .andRespond(withSuccess(NOT_FOUND, MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(CHECK_URL))
                .andRespond(withSuccess(PAID, MediaType.APPLICATION_JSON));

        client.checkTransactionByMd5("md5");
        clock.advance(Duration.ofSeconds(16));

        assertThat(client.checkTransactionByMd5("md5").paid()).isTrue();
        server.verify();
    }

    @Test
    void olderQrIsCheckedLessOften() {
        server.expect(ExpectedCount.times(2), requestTo(CHECK_URL))
                .andRespond(withSuccess(NOT_FOUND, MediaType.APPLICATION_JSON));

        client.checkTransactionByMd5("md5");
        clock.advance(Duration.ofMinutes(3));
        client.checkTransactionByMd5("md5");
        clock.advance(Duration.ofSeconds(30));
        client.checkTransactionByMd5("md5");

        server.verify();
    }

    @Test
    void personAskingCanRecheckSooner() {
        server.expect(ExpectedCount.times(3), requestTo(CHECK_URL))
                .andRespond(withSuccess(NOT_FOUND, MediaType.APPLICATION_JSON));

        client.checkTransactionByMd5("md5");
        clock.advance(Duration.ofMinutes(3));
        client.checkTransactionByMd5("md5");
        clock.advance(Duration.ofSeconds(11));
        client.checkTransactionByMd5("md5");
        client.checkTransactionByMd5("md5", true);

        server.verify();
    }

    @Test
    void dailyLimitPausesChecksUntilMidnightInPhnomPenh() {
        server.expect(ExpectedCount.once(), requestTo(CHECK_URL))
                .andRespond(withSuccess(DAILY_LIMIT, MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(CHECK_URL))
                .andRespond(withSuccess(PAID, MediaType.APPLICATION_JSON));

        BakongTransactionCheckResult limited = client.checkTransactionByMd5("md5");
        assertThat(limited.failed()).isTrue();
        assertThat(limited.message()).contains("daily limit");

        clock.advance(Duration.ofHours(1));
        assertThat(client.checkTransactionByMd5("other-md5", true).failed()).isTrue();

        // 2026-10-04T03:00Z is 10:00 in Phnom Penh; midnight there is 2026-10-04T17:00Z.
        clock.set(Instant.parse("2026-10-04T17:00:01Z"));
        assertThat(client.checkTransactionByMd5("md5").paid()).isTrue();
        server.verify();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            MutableClock outer = this;
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return zone;
                }

                @Override
                public Clock withZone(ZoneId other) {
                    return outer.withZone(other);
                }

                @Override
                public Instant instant() {
                    return outer.now;
                }
            };
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
