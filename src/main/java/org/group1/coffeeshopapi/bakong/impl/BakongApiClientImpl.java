package org.group1.coffeeshopapi.bakong.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongTokenService;
import org.group1.coffeeshopapi.bakong.dto.BakongCheckTransactionResponse;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
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
            // NBC tokens last about 90 days. Rather than let every confirmation fail until
            // someone pastes in a new one, renew against the registered email and try once more.
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

    /**
     * A non-zero {@code responseCode} here is a real answer, not an error — for an unpaid order
     * it is "Transaction could not be found", which is exactly what a QR nobody has scanned yet
     * looks like.
     */
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
}
