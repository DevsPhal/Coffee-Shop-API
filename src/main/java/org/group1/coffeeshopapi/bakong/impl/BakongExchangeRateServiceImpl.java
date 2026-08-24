package org.group1.coffeeshopapi.bakong.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.dto.response.BakongExchangeRateResponse;
import org.group1.coffeeshopapi.bakong.entity.BakongExchangeRate;
import org.group1.coffeeshopapi.bakong.repository.BakongExchangeRateRepository;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BakongExchangeRateServiceImpl implements BakongExchangeRateService {

    private final BakongExchangeRateRepository repository;
    private final BakongProperties bakongProperties;
    private final ActorLookupService actorLookupService;

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
                .orElseGet(() -> new BakongExchangeRateResponse(bakongProperties.getKhrPerUsdRate(), null, null, null, null));
    }

    @Override
    @Transactional
    public BakongExchangeRateResponse updateRate(BigDecimal khrPerUsdRate, UUID updatedByAdminId) {
        if (khrPerUsdRate == null || khrPerUsdRate.signum() <= 0) {
            throw new InvalidOperationException("Exchange rate must be greater than zero");
        }

        BakongExchangeRate entity = repository.findById(BakongExchangeRate.SINGLETON_ID)
                .orElseGet(BakongExchangeRate::new);
        entity.setKhrPerUsdRate(khrPerUsdRate);
        entity.setUpdatedByAdminId(updatedByAdminId);
        return toResponse(repository.save(entity));
    }

    private BakongExchangeRateResponse toResponse(BakongExchangeRate entity) {
        ActorSummary actor = actorLookupService.resolve(entity.getUpdatedByAdminId());
        return new BakongExchangeRateResponse(
                entity.getKhrPerUsdRate(),
                entity.getUpdatedByAdminId(),
                actor != null ? actor.name() : null,
                actor != null ? actor.role() : null,
                entity.getUpdatedAt());
    }
}
