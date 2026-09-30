package org.group1.coffeeshopapi.payway.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.PaywayProperties;
import org.group1.coffeeshopapi.payway.PaywayApiClient;
import org.group1.coffeeshopapi.payway.dto.PaywayCheckTransactionResponse;
import org.group1.coffeeshopapi.payway.dto.PaywayPurchaseResponse;
import org.group1.coffeeshopapi.payway.dto.PaywayPurchaseResult;
import org.group1.coffeeshopapi.payway.dto.PaywayTransactionCheckResult;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaywayApiClientImpl implements PaywayApiClient {

    private static final String PURCHASE_PATH = "/api/payment-gateway/v1/payments/purchase";
    private static final String CHECK_PATH = "/api/payment-gateway/v1/payments/check-transaction-2";
    // Makes PayWay return an abamobilebank:// link that opens ABA Mobile on the payment.
    private static final String PAYMENT_OPTION = "abapay_khqr_deeplink";
    private static final String CURRENCY = "USD";
    private static final int APPROVED = 0;
    private static final DateTimeFormatter REQ_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RestClient paywayRestClient;
    private final PaywayProperties paywayProperties;

    @Override
    public PaywayPurchaseResult createAbaDeeplink(String tranId, BigDecimal amountUsd) {
        requireConfigured();
        String reqTime = reqTime();
        String merchantId = paywayProperties.getMerchantId();
        String amount = amountUsd.setScale(2, RoundingMode.HALF_UP).toPlainString();
        String lifetime = String.valueOf(paywayProperties.getLifetimeMinutes());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("req_time", reqTime);
        form.add("merchant_id", merchantId);
        form.add("tran_id", tranId);
        form.add("amount", amount);
        form.add("payment_option", PAYMENT_OPTION);
        form.add("currency", CURRENCY);
        form.add("lifetime", lifetime);
        // PayWay's fixed hash order; fields we don't send count as empty.
        form.add("hash", sign(reqTime + merchantId + tranId + amount + PAYMENT_OPTION + CURRENCY + lifetime));

        try {
            PaywayPurchaseResponse response = paywayRestClient.post()
                    .uri(PURCHASE_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(PaywayPurchaseResponse.class);
            if (response == null || response.status() == null) {
                return PaywayPurchaseResult.failed("Empty response from ABA PayWay");
            }
            if (!response.status().isSuccess() || response.abapayDeeplink() == null) {
                log.warn("PayWay refused purchase tran_id={}: {} {}", tranId,
                        response.status().code(), response.status().message());
                return PaywayPurchaseResult.failed("ABA PayWay declined the payment: " + response.status().message());
            }
            return PaywayPurchaseResult.success(response.abapayDeeplink());
        } catch (RestClientException e) {
            log.warn("PayWay purchase call failed for tran_id={}", tranId, e);
            return PaywayPurchaseResult.failed("Could not reach ABA PayWay. Please try again.");
        }
    }

    @Override
    public PaywayTransactionCheckResult checkTransaction(String tranId) {
        requireConfigured();
        String reqTime = reqTime();
        String merchantId = paywayProperties.getMerchantId();
        Map<String, String> body = Map.of(
                "req_time", reqTime,
                "merchant_id", merchantId,
                "tran_id", tranId,
                "hash", sign(reqTime + merchantId + tranId));

        try {
            PaywayCheckTransactionResponse response = paywayRestClient.post()
                    .uri(CHECK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(PaywayCheckTransactionResponse.class);
            if (response == null || response.status() == null || !response.status().isSuccess()
                    || response.data() == null) {
                String message = response == null || response.status() == null ? "empty response" : response.status().message();
                return PaywayTransactionCheckResult.notPaid(message);
            }
            var data = response.data();
            boolean approved = data.paymentStatusCode() != null && data.paymentStatusCode() == APPROVED
                    && "APPROVED".equalsIgnoreCase(data.paymentStatus());
            return approved
                    ? PaywayTransactionCheckResult.paid(data.approvalCode(), data.paymentAmount())
                    : PaywayTransactionCheckResult.notPaid(data.paymentStatus());
        } catch (RestClientException e) {
            log.warn("PayWay check-transaction call failed for tran_id={}", tranId, e);
            return PaywayTransactionCheckResult.notPaid("Could not reach ABA PayWay.");
        }
    }

    private void requireConfigured() {
        if (!paywayProperties.isConfigured()) {
            throw new InvalidOperationException("ABA PayWay is not configured");
        }
    }

    private String reqTime() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(REQ_TIME);
    }

    // Base64 of HMAC-SHA512 keyed with the merchant's API key.
    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(paywayProperties.getApiKey().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA512 is unavailable", e);
        }
    }
}
