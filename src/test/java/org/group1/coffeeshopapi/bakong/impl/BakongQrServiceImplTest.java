package org.group1.coffeeshopapi.bakong.impl;

import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.dto.BakongQrResult;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BakongQrServiceImplTest {

    @Mock private BakongExchangeRateService exchangeRateService;
    private BakongQrServiceImpl service;

    @BeforeEach
    void setUp() {
        BakongProperties properties = new BakongProperties();
        properties.setToken("test-token");
        properties.setAccountId("cafe590st@aclb");
        properties.setMerchantName("590st Cafe");
        properties.setMerchantCity("Phnom Penh");
        properties.setCurrency(Currency.USD);
        service = new BakongQrServiceImpl(properties, exchangeRateService);
    }

    @Test
    void aRielQrAsksForTheTotalRoundedDownWhenTheLastTwoDigitsAreFiftyOrLess() {
        when(exchangeRateService.getCurrentRate()).thenReturn(new BigDecimal("4100"));

        // $14.50 at 4,100 is 59,450 riel: last two digits 50 round down.
        BakongQrResult qr = service.generateQr(new BigDecimal("14.50"), "ORD-1", Currency.KHR, inTenMinutes());

        assertThat(qr.currency()).isEqualTo(Currency.KHR);
        assertThat(qr.amount()).isEqualByComparingTo("59400");
        assertThat(qr.exchangeRate()).isEqualByComparingTo("4100");
        assertThat(qr.qrString()).isNotBlank();
    }

    @Test
    void aRielQrRoundsUpWhenTheLastTwoDigitsAre51OrMore() {
        when(exchangeRateService.getCurrentRate()).thenReturn(new BigDecimal("4100"));

        // $1.19 at 4,100 is 4,879 riel.
        BakongQrResult qr = service.generateQr(new BigDecimal("1.19"), "ORD-2", Currency.KHR, inTenMinutes());

        assertThat(qr.amount()).isEqualByComparingTo("4900");
    }

    @Test
    void aDollarQrKeepsTheExactDollarAmountAndNeedsNoRate() {
        BakongQrResult qr = service.generateQr(new BigDecimal("1.19"), "ORD-3", Currency.USD, inTenMinutes());

        assertThat(qr.currency()).isEqualTo(Currency.USD);
        assertThat(qr.amount()).isEqualByComparingTo("1.19");
        assertThat(qr.exchangeRate()).isNull();
        verifyNoInteractions(exchangeRateService);
    }

    @Test
    void anAmountThatRoundsToNoRielIsRefused() {
        when(exchangeRateService.getCurrentRate()).thenReturn(new BigDecimal("4100"));

        // $0.01 is 41 riel, which rounds down to 0.
        assertThatThrownBy(() -> service.generateQr(new BigDecimal("0.01"), "ORD-4", Currency.KHR, inTenMinutes()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("USD QR");
    }

    @Test
    void aRielQrWithoutARateFailsClearly() {
        when(exchangeRateService.getCurrentRate()).thenReturn(null);

        assertThatThrownBy(() -> service.generateQr(BigDecimal.TEN, "ORD-5", Currency.KHR, inTenMinutes()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exchange rate");
    }

    private static LocalDateTime inTenMinutes() {
        return LocalDateTime.now().plusMinutes(10);
    }
}
