package org.group1.coffeeshopapi.bakong.impl;

import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongTokenService;
import org.group1.coffeeshopapi.bakong.dto.BakongCheckTransactionResponse;
import org.group1.coffeeshopapi.bakong.dto.BakongDeeplinkResult;
import org.group1.coffeeshopapi.bakong.dto.BakongGenerateDeeplinkResponse;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class BakongApiClientImpl implements BakongApiClient {

    // Bakong errorCode values for check_transaction_by_md5.
    private static final int ERROR_NOT_FOUND = 1;
    private static final int ERROR_TRANSACTION_FAILED = 3;
    private static final int ERROR_UNAUTHORIZED = 6;
    private static final int ERROR_DAILY_LIMIT = 17;

    // Bakong allows only ~100 checks a day per token, shared by every open payment screen, the POS and the
    // background watcher. Each QR is therefore checked on one shared cadence that slows down as the QR ages:
    // customers usually pay within the first minute or two.
    private static final Duration PROMPT_INTERVAL = Duration.ofSeconds(10);
    private static final Duration FRESH_QR_AGE = Duration.ofMinutes(2);
    private static final Duration FRESH_QR_INTERVAL = Duration.ofSeconds(15);
    private static final Duration ACTIVE_QR_AGE = Duration.ofMinutes(15);
    private static final Duration ACTIVE_QR_INTERVAL = Duration.ofMinutes(1);
    private static final Duration OLD_QR_INTERVAL = Duration.ofMinutes(5);
    private static final Duration FORGET_AFTER = Duration.ofHours(3);
    private static final ZoneId BAKONG_ZONE = ZoneId.of("Asia/Phnom_Penh");
    private static final String DAILY_LIMIT_MESSAGE =
            "Bakong's daily limit for payment checks has been reached, so payments can't be confirmed "
                    + "automatically until midnight.";

    private final RestClient bakongRestClient;
    private final BakongProperties bakongProperties;
    private final BakongTokenService tokenService;
    private final Clock clock;

    private final Map<String, CheckState> checks = new ConcurrentHashMap<>();
    private volatile Instant dailyLimitResetsAt;
    private volatile Instant lastPrunedAt = Instant.EPOCH;

    @Autowired
    public BakongApiClientImpl(RestClient bakongRestClient, BakongProperties bakongProperties,
                               BakongTokenService tokenService) {
        this(bakongRestClient, bakongProperties, tokenService, Clock.systemUTC());
    }

    BakongApiClientImpl(RestClient bakongRestClient, BakongProperties bakongProperties,
                        BakongTokenService tokenService, Clock clock) {
        this.bakongRestClient = bakongRestClient;
        this.bakongProperties = bakongProperties;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    @Override
    public BakongTransactionCheckResult checkTransactionByMd5(String md5Hash) {
        return checkTransactionByMd5(md5Hash, false);
    }

    @Override
    public BakongTransactionCheckResult checkTransactionByMd5(String md5Hash, boolean promptly) {
        if (!bakongProperties.isConfigured()) {
            throw new InvalidOperationException("Bakong payment is not configured");
        }

        Instant now = clock.instant();
        Instant resetsAt = dailyLimitResetsAt;
        if (resetsAt != null && now.isBefore(resetsAt)) {
            return BakongTransactionCheckResult.failed(DAILY_LIMIT_MESSAGE);
        }
        forgetOldChecks(now);

        CheckState state = checks.computeIfAbsent(md5Hash, key -> new CheckState(now));
        synchronized (state) {
            if (state.lastResult != null
                    && (state.lastResult.paid() || now.isBefore(state.lastCheckedAt.plus(interval(state, now, promptly))))) {
                return state.lastResult;
            }
            BakongTransactionCheckResult result = askBakong(md5Hash);
            if (dailyLimitResetsAt == null || !now.isBefore(dailyLimitResetsAt)) {
                state.lastCheckedAt = now;
                state.lastResult = result;
            }
            return result;
        }
    }

    private Duration interval(CheckState state, Instant now, boolean promptly) {
        if (promptly) {
            return PROMPT_INTERVAL;
        }
        Duration age = Duration.between(state.firstSeenAt, now);
        if (age.compareTo(FRESH_QR_AGE) < 0) {
            return FRESH_QR_INTERVAL;
        }
        return age.compareTo(ACTIVE_QR_AGE) < 0 ? ACTIVE_QR_INTERVAL : OLD_QR_INTERVAL;
    }

    private void forgetOldChecks(Instant now) {
        if (now.isBefore(lastPrunedAt.plus(Duration.ofMinutes(1)))) {
            return;
        }
        lastPrunedAt = now;
        checks.values().removeIf(state -> state.firstSeenAt.isBefore(now.minus(FORGET_AFTER)));
    }

    private BakongTransactionCheckResult askBakong(String md5Hash) {
        try {
            BakongCheckTransactionResponse response = call(md5Hash, tokenService.currentToken());
            if (!isTokenRejected(response)) {
                return interpret(md5Hash, response);
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            // Renewed below.
        } catch (RestClientException e) {
            return unreachable(md5Hash, e);
        }

        log.info("Bakong rejected the access token; attempting to renew it");
        String renewed = tokenService.renew();
        if (renewed == null) {
            return BakongTransactionCheckResult.failed(
                    "The Bakong access token has expired and could not be renewed automatically. "
                            + "Check BAKONG_EMAIL and BAKONG_TOKEN.");
        }
        try {
            BakongCheckTransactionResponse response = call(md5Hash, renewed);
            if (isTokenRejected(response)) {
                log.error("Bakong still rejected the access token after renewal: {}", response.responseMessage());
                return BakongTransactionCheckResult.failed("Bakong rejected the access token even after renewing it.");
            }
            return interpret(md5Hash, response);
        } catch (HttpClientErrorException.Unauthorized retryFailure) {
            log.error("Bakong still rejected the access token after renewal", retryFailure);
            return BakongTransactionCheckResult.failed(
                    "Bakong rejected the access token even after renewing it.");
        } catch (RestClientException retryFailure) {
            return unreachable(md5Hash, retryFailure);
        }
    }

    private BakongCheckTransactionResponse call(String md5Hash, String token) {
        return bakongRestClient.post()
                .uri("/v1/check_transaction_by_md5")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("md5", md5Hash))
                .retrieve()
                .body(BakongCheckTransactionResponse.class);
    }

    // Bakong can answer an expired or invalid token with HTTP 200 and errorCode 6 instead of a 401.
    private boolean isTokenRejected(BakongCheckTransactionResponse response) {
        return response != null && response.responseCode() != 0
                && response.errorCode() != null && response.errorCode() == ERROR_UNAUTHORIZED;
    }

    private BakongTransactionCheckResult interpret(String md5Hash, BakongCheckTransactionResponse response) {
        if (response == null) {
            return BakongTransactionCheckResult.failed("Empty response from Bakong");
        }
        if (response.responseCode() == 0 && response.data() != null) {
            return BakongTransactionCheckResult.paid(
                    response.data().hash(),
                    parseAmount(response.data().amount()),
                    response.data().currency(),
                    response.responseMessage());
        }
        Integer errorCode = response.errorCode();
        if (errorCode != null && errorCode == ERROR_DAILY_LIMIT) {
            Instant resetsAt = LocalDate.now(clock.withZone(BAKONG_ZONE)).plusDays(1)
                    .atStartOfDay(BAKONG_ZONE).toInstant();
            dailyLimitResetsAt = resetsAt;
            log.error("Bakong daily request limit reached ({}); pausing payment checks until {}",
                    response.responseMessage(), resetsAt);
            return BakongTransactionCheckResult.failed(DAILY_LIMIT_MESSAGE);
        }
        if (errorCode == null || errorCode == ERROR_NOT_FOUND || errorCode == ERROR_TRANSACTION_FAILED) {
            return BakongTransactionCheckResult.notPaid(response.responseMessage());
        }
        // Anything else is Bakong refusing the request, not "unpaid" — surface it instead of waiting forever.
        log.error("Bakong check_transaction_by_md5 returned errorCode={} for md5={}: {}",
                errorCode, md5Hash, response.responseMessage());
        return BakongTransactionCheckResult.failed(response.responseMessage() != null
                ? "Bakong could not check the payment: " + response.responseMessage()
                : "Bakong could not check the payment (error " + errorCode + ").");
    }

    private BakongTransactionCheckResult unreachable(String md5Hash, RestClientException e) {
        if (e instanceof HttpClientErrorException.Forbidden) {
            log.error("Bakong refused check_transaction_by_md5 with 403 for md5={} — this server's IP is "
                    + "likely outside Cambodia; route BAKONG_BASE_URL through a relay in Cambodia", md5Hash);
            return BakongTransactionCheckResult.failed(
                    "Bakong refused the payment check (403). Its API only accepts calls from servers in Cambodia.");
        }
        log.warn("Bakong check_transaction_by_md5 call failed for md5={}", md5Hash, e);
        return BakongTransactionCheckResult.failed("Could not reach the Bakong API. Please try again.");
    }

    private BigDecimal parseAmount(String amount) {
        try {
            return amount == null ? null : new BigDecimal(amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public BakongDeeplinkResult generateDeeplink(String qrString, String callbackUrl) {
        if (!bakongProperties.isConfigured()) {
            throw new InvalidOperationException("Bakong payment is not configured");
        }

        try {
            return interpretDeeplink(callDeeplink(qrString, callbackUrl, tokenService.currentToken()));
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("Bakong rejected the access token; attempting to renew it");
            String renewed = tokenService.renew();
            if (renewed == null) {
                return BakongDeeplinkResult.failed(
                        "The Bakong access token has expired and could not be renewed automatically. "
                                + "Check BAKONG_EMAIL and BAKONG_TOKEN.");
            }
            try {
                return interpretDeeplink(callDeeplink(qrString, callbackUrl, renewed));
            } catch (HttpClientErrorException.Unauthorized retryFailure) {
                log.error("Bakong still rejected the access token after renewal", retryFailure);
                return BakongDeeplinkResult.failed("Bakong rejected the access token even after renewing it.");
            } catch (RestClientException retryFailure) {
                return deeplinkUnreachable(retryFailure);
            }
        } catch (RestClientException e) {
            return deeplinkUnreachable(e);
        }
    }

    private BakongGenerateDeeplinkResponse callDeeplink(String qrString, String callbackUrl, String token) {
        Map<String, Object> sourceInfo = new LinkedHashMap<>();
        sourceInfo.put("appIconUrl", bakongProperties.getAppIconUrl());
        sourceInfo.put("appName", bakongProperties.getMerchantName());
        sourceInfo.put("appDeepLinkCallback", callbackUrl);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("qr", qrString);
        body.put("sourceInfo", sourceInfo);

        return bakongRestClient.post()
                .uri("/v1/generate_deeplink_by_qr")
                .header("Authorization", "Bearer " + token)
                .body(body)
                .retrieve()
                .body(BakongGenerateDeeplinkResponse.class);
    }

    private BakongDeeplinkResult interpretDeeplink(BakongGenerateDeeplinkResponse response) {
        if (response == null) {
            return BakongDeeplinkResult.failed("Empty response from Bakong");
        }
        if (response.responseCode() != 0 || response.data() == null || response.data().shortLink() == null) {
            return BakongDeeplinkResult.declined(response.responseMessage());
        }
        return BakongDeeplinkResult.success(response.data().shortLink(), response.responseMessage());
    }

    private BakongDeeplinkResult deeplinkUnreachable(RestClientException e) {
        log.warn("Bakong generate_deeplink_by_qr call failed", e);
        return BakongDeeplinkResult.failed("Could not reach the Bakong API. Please try again.");
    }

    private static final class CheckState {
        private final Instant firstSeenAt;
        private Instant lastCheckedAt;
        private BakongTransactionCheckResult lastResult;

        private CheckState(Instant firstSeenAt) {
            this.firstSeenAt = firstSeenAt;
        }
    }
}
