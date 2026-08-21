package org.group1.coffeeshopapi.bakong.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.dto.BakongCheckTransactionResponse;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.stereotype.Service;
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

    @Override
    public BakongTransactionCheckResult checkTransactionByMd5(String md5Hash) {
        if (!bakongProperties.isConfigured()) {
            throw new InvalidOperationException("Bakong payment is not configured");
        }

        try {
            BakongCheckTransactionResponse response = bakongRestClient.post()
                    .uri("/v1/check_transaction_by_md5")
                    .header("Authorization", "Bearer " + bakongProperties.getToken())
                    .body(Map.of("md5", md5Hash))
                    .retrieve()
                    .body(BakongCheckTransactionResponse.class);

            if (response == null || response.responseCode() != 0 || response.data() == null) {
                String message = response == null ? "Empty response from Bakong" : response.responseMessage();
                return new BakongTransactionCheckResult(false, null, null, null, message);
            }

            BigDecimal amount = parseAmount(response.data().amount());
            return new BakongTransactionCheckResult(
                    true, response.data().hash(), amount, response.data().currency(), response.responseMessage());
        } catch (RestClientException e) {
            log.warn("Bakong check_transaction_by_md5 call failed for md5={}", md5Hash, e);
            return new BakongTransactionCheckResult(false, null, null, null, "Not yet paid or Bakong API unavailable");
        }
    }

    private BigDecimal parseAmount(String amount) {
        try {
            return amount == null ? null : new BigDecimal(amount);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
