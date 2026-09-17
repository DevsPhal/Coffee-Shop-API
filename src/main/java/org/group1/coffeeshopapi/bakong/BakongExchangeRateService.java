package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;

import java.math.BigDecimal;

public interface BakongExchangeRateService {

    // The admin-set rate if one exists, else the configured default.
    BigDecimal getCurrentRate();

    BakongExchangeRateResponse getRateInfo();

    // marketRate is optional — pass null to leave it unchanged.
    BakongExchangeRateResponse updateRate(BigDecimal khrPerUsdRate, BigDecimal marketRate, Admin actorAdmin);
}
