package org.group1.coffeeshopapi.bakong.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barista/bakong/exchange-rate")
@RequiredArgsConstructor
@Tag(name = "Barista Exchange Rate", description = "Barista only: read the USD-to-KHR rate to take cash or Bakong payments in riel")
@SecurityRequirement(name = "bearerAuth")
public class BaristaExchangeRateController {

    private final BakongExchangeRateService exchangeRateService;

    @GetMapping
    public ApiResponse<BakongExchangeRateResponse> get() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, exchangeRateService.getRateInfo());
    }
}
