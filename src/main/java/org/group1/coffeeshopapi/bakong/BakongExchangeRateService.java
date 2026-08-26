package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface BakongExchangeRateService {

    /** The rate to use right now: the admin-set override if one exists, else the configured default. */
    BigDecimal getCurrentRate();

    BakongExchangeRateResponse getRateInfo();

    // marketRate is optional — pass null to leave the stored market rate unchanged.
    BakongExchangeRateResponse updateRate(BigDecimal khrPerUsdRate, BigDecimal marketRate, UUID updatedByAdminId);
}
