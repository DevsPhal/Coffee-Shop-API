package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;

import java.math.BigDecimal;

public interface BakongExchangeRateService {

    /** The rate to use right now: the admin-set override if one exists, else the configured default. */
    BigDecimal getCurrentRate();

    BakongExchangeRateResponse getRateInfo();

    // marketRate is optional — pass null to leave the stored market rate unchanged. actorAdmin
    // is null when the Super Admin is the one acting — see CurrentActor.adminRef().
    BakongExchangeRateResponse updateRate(BigDecimal khrPerUsdRate, BigDecimal marketRate, Admin actorAdmin);
}
