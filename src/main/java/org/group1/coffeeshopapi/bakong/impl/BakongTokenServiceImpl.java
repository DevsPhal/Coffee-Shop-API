package org.group1.coffeeshopapi.bakong.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.bakong.BakongTokenService;
import org.group1.coffeeshopapi.bakong.dto.BakongRenewTokenResponse;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class BakongTokenServiceImpl implements BakongTokenService {

    private final RestClient bakongRestClient;
    private final BakongProperties bakongProperties;

    // Null until a renewal succeeds, then holds the renewed token for the rest of this process's
    // life. Not written back to config — a restart just renews again on the next failure.
    private final AtomicReference<String> renewedToken = new AtomicReference<>();

    @Override
    public String currentToken() {
        String renewed = renewedToken.get();
        return renewed != null ? renewed : bakongProperties.getToken();
    }

    @Override
    public String renew() {
        String email = bakongProperties.getEmail();
        if (email == null || email.isBlank()) {
            log.warn("Bakong token was rejected but BAKONG_EMAIL is not set, so it cannot be renewed automatically");
            return null;
        }

        try {
            // NBC issues a new token and also emails it; the response body is the copy we keep.
            BakongRenewTokenResponse response = bakongRestClient.post()
                    .uri("/v1/renew_token")
                    .body(Map.of("email", email))
                    .retrieve()
                    .body(BakongRenewTokenResponse.class);

            if (response == null || response.responseCode() != 0
                    || response.data() == null || response.data().token() == null) {
                String message = response == null ? "empty response" : response.responseMessage();
                log.error("Bakong refused to renew the token for {}: {}", email, message);
                return null;
            }

            renewedToken.set(response.data().token());
            log.info("Renewed the Bakong access token for {}", email);
            return response.data().token();
        } catch (RestClientException e) {
            log.error("Could not reach Bakong to renew the access token for {}", email, e);
            return null;
        }
    }
}
