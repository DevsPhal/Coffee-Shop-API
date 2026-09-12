package org.group1.coffeeshopapi.bakong.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;
import org.group1.coffeeshopapi.bakong.entity.BakongExchangeRate;
import org.group1.coffeeshopapi.bakong.repository.BakongExchangeRateRepository;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BakongExchangeRateServiceImpl implements BakongExchangeRateService {

    private final BakongExchangeRateRepository repository;
    private final BakongProperties bakongProperties;

    @Override
    public BigDecimal getCurrentRate() {
        return repository.findById(BakongExchangeRate.SINGLETON_ID)
                .map(BakongExchangeRate::getKhrPerUsdRate)
                .orElseGet(bakongProperties::getKhrPerUsdRate);
    }

    @Override
    public BakongExchangeRateResponse getRateInfo() {
        return repository.findById(BakongExchangeRate.SINGLETON_ID)
                .map(this::toResponse)
                .orElseGet(() -> new BakongExchangeRateResponse(bakongProperties.getKhrPerUsdRate(), null, null, null, null, null));
    }

    @Override
    @Transactional
    public BakongExchangeRateResponse updateRate(BigDecimal khrPerUsdRate, BigDecimal marketRate, Admin actorAdmin) {
        if (khrPerUsdRate == null || khrPerUsdRate.signum() <= 0) {
            throw new InvalidOperationException("Exchange rate must be greater than zero");
        }

        BakongExchangeRate entity = repository.findById(BakongExchangeRate.SINGLETON_ID)
                .orElseGet(BakongExchangeRate::new);
        entity.setKhrPerUsdRate(khrPerUsdRate);
        if (marketRate != null) {
            entity.setMarketRate(marketRate);
        }
        entity.setUpdatedByAdmin(actorAdmin);
        return toResponse(repository.save(entity));
    }

    // updatedByAdmin is null both for a rate never updated through the app and for a change made
    // by the Super Admin (see BakongExchangeRate's javadoc).
    private BakongExchangeRateResponse toResponse(BakongExchangeRate entity) {
        Admin admin = entity.getUpdatedByAdmin();
        return new BakongExchangeRateResponse(
                entity.getKhrPerUsdRate(),
                entity.getMarketRate(),
                admin != null ? admin.getId() : null,
                admin != null ? admin.getFullName() : null,
                admin != null ? admin.getRole() : null,
                entity.getUpdatedAt());
    }
}
