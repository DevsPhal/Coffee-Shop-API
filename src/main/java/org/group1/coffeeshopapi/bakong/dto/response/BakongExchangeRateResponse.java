package org.group1.coffeeshopapi.bakong.dto.response;

import org.group1.coffeeshopapi.common.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BakongExchangeRateResponse(
        BigDecimal khrPerUsdRate,
        UUID updatedByAdminId,
        String updatedByAdminName,
        Role updatedByAdminRole,
        LocalDateTime updatedAt
) {
}
