package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;

import java.math.BigDecimal;

public interface BakongExchangeRateService {

    BigDecimal getCurrentRate();

    BakongExchangeRateResponse getRateInfo();

    BakongExchangeRateResponse updateRate(BigDecimal khrPerUsdRate, BigDecimal marketRate, Admin actorAdmin);
}
