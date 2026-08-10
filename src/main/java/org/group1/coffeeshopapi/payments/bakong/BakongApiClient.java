package org.group1.coffeeshopapi.payments.bakong;

import java.util.Map;

import org.group1.coffeeshopapi.common.exception.InvalidPaymentException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.RequiredArgsConstructor;

/** Talks to Bakong's Open API to check whether a KHQR transaction has been paid. */
@Component
@RequiredArgsConstructor
public class BakongApiClient {

    private static final String CHECK_TRANSACTION_PATH = "/v1/check_transaction_by_md5";

    private final RestTemplate bakongRestTemplate;

    @Value("${bakong.token}")
    private String token;

    public BakongTransactionStatus checkTransactionByMd5(String md5Hash) {
        if (token == null || token.isBlank()) {
            throw new InvalidPaymentException("Bakong API token is not configured (set BAKONG_TOKEN)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("md5", md5Hash), headers);

        try {
            CheckTransactionResponse response = bakongRestTemplate.postForObject(
                    CHECK_TRANSACTION_PATH, request, CheckTransactionResponse.class);

            if (response == null) {
                return new BakongTransactionStatus(false, null, "Empty response from Bakong");
            }
            boolean paid = response.responseCode() != null && response.responseCode() == 0;
            String transactionHash = paid && response.data() != null ? response.data().hash() : null;
            return new BakongTransactionStatus(paid, transactionHash, response.responseMessage());
        } catch (RestClientException e) {
            return new BakongTransactionStatus(false, null, "Not yet paid or Bakong API unavailable: " + e.getMessage());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CheckTransactionResponse(Integer responseCode, String responseMessage, TransactionData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TransactionData(String hash, String fromAccountId, String toAccountId,
                                     String currency, Double amount) {
    }
}
