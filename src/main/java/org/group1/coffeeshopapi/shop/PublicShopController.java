package org.group1.coffeeshopapi.shop;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class PublicShopController {
    private final ShopSettingsService settings;
    private final BakongExchangeRateService exchangeRates;

    public record SettingsResponse(BigDecimal deliveryFee, BigDecimal khrPerUsdRate) {}

    @GetMapping("/settings")
    public ApiResponse<SettingsResponse> settings() {
        return ApiResponse.of(HttpStatus.OK, "Success",
                new SettingsResponse(settings.deliveryFee(), exchangeRates.getCurrentRate()));
    }
}
