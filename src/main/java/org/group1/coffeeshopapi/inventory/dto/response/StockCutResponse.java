package org.group1.coffeeshopapi.inventory.dto.response;

import org.group1.coffeeshopapi.common.enums.StockStrategy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StockCutResponse(
        UUID movementId,
        UUID productId,
        StockStrategy strategy,
        BigDecimal quantityCut,
        BigDecimal remainingOnHand,
        List<BatchConsumptionResponse> consumptions
) {
}