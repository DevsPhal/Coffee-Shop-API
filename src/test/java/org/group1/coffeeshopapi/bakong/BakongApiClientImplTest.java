package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.bakong.impl.BakongApiClientImpl;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BakongApiClientImplTest {

    private static final String CHECK_URL = "https://bakong.test/v1/check_transaction_by_md5";

    private MockRestServiceServer server;
    private BakongTokenService tokenService;
    private BakongApiClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://bakong.test");
        server = MockRestServiceServer.bindTo(builder).build();

        BakongProperties properties = new BakongProperties();
        properties.setToken("old-token");
        properties.setAccountId("shop@bank");
        properties.setMerchantName("590st Cafe");

        tokenService = mock(BakongTokenService.class);
        when(tokenService.currentToken()).thenReturn("old-token");

        client = new BakongApiClientImpl(builder.build(), properties, tokenService);
    }

    @Test
    void paidTransactionIsReportedAsPaid() {
        server.expect(requestTo(CHECK_URL)).andRespond(withSuccess("""
                {"responseCode":0,"responseMessage":"Success","errorCode":null,
                 "data":{"hash":"tx-1","amount":"2.5","currency":"USD"}}""", MediaType.APPLICATION_JSON));

        BakongTransactionCheckResult result = client.checkTransactionByMd5("md5");

        assertThat(result.paid()).isTrue();
        assertThat(result.transactionHash()).isEqualTo("tx-1");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("2.5"));
    }

    @Test
    void transactionNotFoundIsNotPaidYet() {
        server.expect(requestTo(CHECK_URL)).andRespond(withSuccess("""
                {"responseCode":1,"responseMessage":"Transaction could not be found.","errorCode":1,"data":null}""",
                MediaType.APPLICATION_JSON));

        BakongTransactionCheckResult result = client.checkTransactionByMd5("md5");

        assertThat(result.paid()).isFalse();
        assertThat(result.failed()).isFalse();
        verify(tokenService, never()).renew();
    }

    @Test
    void tokenRejectedInBodyIsRenewedAndRetried() {
        when(tokenService.renew()).thenReturn("new-token");
        server.expect(requestTo(CHECK_URL)).andExpect(header("Authorization", "Bearer old-token"))
                .andRespond(withSuccess("""
                        {"responseCode":1,"responseMessage":"Unauthorized","errorCode":6,"data":null}""",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(CHECK_URL)).andExpect(header("Authorization", "Bearer new-token"))
                .andRespond(withSuccess("""
                        {"responseCode":0,"responseMessage":"Success",
                         "data":{"hash":"tx-2","amount":"1.00","currency":"USD"}}""", MediaType.APPLICATION_JSON));

        BakongTransactionCheckResult result = client.checkTransactionByMd5("md5");

        assertThat(result.paid()).isTrue();
        server.verify();
    }

    @Test
    void tokenRejectedWithHttp401IsRenewedAndRetried() {
        when(tokenService.renew()).thenReturn("new-token");
        server.expect(requestTo(CHECK_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(CHECK_URL)).andExpect(header("Authorization", "Bearer new-token"))
                .andRespond(withSuccess("""
                        {"responseCode":0,"data":{"hash":"tx-3","amount":"1","currency":"USD"}}""",
                        MediaType.APPLICATION_JSON));

        assertThat(client.checkTransactionByMd5("md5").paid()).isTrue();
    }

    @Test
    void tokenThatCannotBeRenewedFailsInsteadOfLookingUnpaid() {
        when(tokenService.renew()).thenReturn(null);
        server.expect(requestTo(CHECK_URL)).andRespond(withSuccess("""
                {"responseCode":1,"responseMessage":"Unauthorized","errorCode":6,"data":null}""",
                MediaType.APPLICATION_JSON));

        BakongTransactionCheckResult result = client.checkTransactionByMd5("md5");

        assertThat(result.failed()).isTrue();
        assertThat(result.message()).contains("BAKONG_EMAIL");
    }

    @Test
    void unknownErrorCodeFailsInsteadOfLookingUnpaid() {
        server.expect(requestTo(CHECK_URL)).andRespond(withSuccess("""
                {"responseCode":1,"responseMessage":"Something else","errorCode":99,"data":null}""",
                MediaType.APPLICATION_JSON));

        BakongTransactionCheckResult result = client.checkTransactionByMd5("md5");

        assertThat(result.failed()).isTrue();
        assertThat(result.message()).contains("Something else");
    }

    @Test
    void geoBlockedRequestFails() {
        server.expect(requestTo(CHECK_URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        BakongTransactionCheckResult result = client.checkTransactionByMd5("md5");

        assertThat(result.failed()).isTrue();
        assertThat(result.message()).contains("403");
    }
}
