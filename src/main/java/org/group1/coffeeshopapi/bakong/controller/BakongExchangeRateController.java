package org.group1.coffeeshopapi.bakong.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.dto.request.UpdateBakongExchangeRateRequest;
import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bakong/exchange-rate")
@RequiredArgsConstructor
@Tag(name = "Bakong Exchange Rate", description = "Admin only: view and update the USD-to-KHR rate used to convert order totals for Bakong KHR QR codes")
@SecurityRequirement(name = "bearerAuth")
public class BakongExchangeRateController {

    private final BakongExchangeRateService exchangeRateService;
    private final CurrentActor currentActor;

    @GetMapping
    public ApiResponse<BakongExchangeRateResponse> get() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, exchangeRateService.getRateInfo());
    }

    @PutMapping
    public ApiResponse<BakongExchangeRateResponse> update(@Valid @RequestBody UpdateBakongExchangeRateRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Exchange rate updated successfully.",
                exchangeRateService.updateRate(request.khrPerUsdRate(), request.marketRate(), currentActor.id()));
    }
}
