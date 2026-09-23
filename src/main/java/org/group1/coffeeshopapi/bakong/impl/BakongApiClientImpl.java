package org.group1.coffeeshopapi.bakong.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongTokenService;
import org.group1.coffeeshopapi.bakong.dto.BakongCheckTransactionResponse;
import org.group1.coffeeshopapi.bakong.dto.BakongDeeplinkResult;
import org.group1.coffeeshopapi.bakong.dto.BakongGenerateDeeplinkResponse;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BakongApiClientImpl implements BakongApiClient {

    private final RestClient bakongRestClient;
    private final BakongProperties bakongProperties;
    private final BakongTokenService tokenService;

    @Override
    public BakongTransactionCheckResult checkTransactionByMd5(String md5Hash) {
        if (!bakongProperties.isConfigured()) {
            throw new InvalidOperationException("Bakong payment is not configured");
        }

        try {
            return interpret(call(md5Hash, tokenService.currentToken()));
        } catch (HttpClientErrorException.Unauthorized e) {
            // Token expired — renew it and try once more before giving up.
            log.info("Bakong rejected the access token; attempting to renew it");
            String renewed = tokenService.renew();
            if (renewed == null) {
                return BakongTransactionCheckResult.failed(
                        "The Bakong access token has expired and could not be renewed automatically. "
                                + "Check BAKONG_EMAIL and BAKONG_TOKEN.");
            }
            try {
                return interpret(call(md5Hash, renewed));
            } catch (HttpClientErrorException.Unauthorized retryFailure) {
                log.error("Bakong still rejected the access token after renewal", retryFailure);
                return BakongTransactionCheckResult.failed(
                        "Bakong rejected the access token even after renewing it.");
            } catch (RestClientException retryFailure) {
                return unreachable(md5Hash, retryFailure);
            }
        } catch (RestClientException e) {
            return unreachable(md5Hash, e);
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

    // A non-zero responseCode is a real answer, not an error — it just means not paid yet.
    private BakongTransactionCheckResult interpret(BakongCheckTransactionResponse response) {
        if (response == null) {
            return BakongTransactionCheckResult.failed("Empty response from Bakong");
        }
        if (response.responseCode() != 0 || response.data() == null) {
            return BakongTransactionCheckResult.notPaid(response.responseMessage());
        }
        return BakongTransactionCheckResult.paid(
                response.data().hash(),
                parseAmount(response.data().amount()),
                response.data().currency(),
                response.responseMessage());
    }

    private BakongTransactionCheckResult unreachable(String md5Hash, RestClientException e) {
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

    // Lets a customer tap to pay instead of scanning the QR on their own screen.
    @Override
    public BakongDeeplinkResult generateDeeplink(String qrString) {
        if (!bakongProperties.isConfigured()) {
            throw new InvalidOperationException("Bakong payment is not configured");
        }

        try {
            return interpretDeeplink(callDeeplink(qrString, tokenService.currentToken()));
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("Bakong rejected the access token; attempting to renew it");
            String renewed = tokenService.renew();
            if (renewed == null) {
                return BakongDeeplinkResult.failed(
                        "The Bakong access token has expired and could not be renewed automatically. "
                                + "Check BAKONG_EMAIL and BAKONG_TOKEN.");
            }
            try {
                return interpretDeeplink(callDeeplink(qrString, renewed));
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

    private BakongGenerateDeeplinkResponse callDeeplink(String qrString, String token) {
        Map<String, Object> sourceInfo = new LinkedHashMap<>();
        sourceInfo.put("appIconUrl", bakongProperties.getAppIconUrl());
        sourceInfo.put("appName", bakongProperties.getMerchantName());
        sourceInfo.put("appDeepLinkCallback", bakongProperties.getDeeplinkCallbackUrl());

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
}
