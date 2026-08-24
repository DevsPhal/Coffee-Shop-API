package org.group1.coffeeshopapi.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// One line of a stock-cut breakdown: how much was taken from a single batch, and at what cost.
public record BatchConsumptionResponse(
        UUID batchId,
        LocalDateTime batchReceivedAt,
        BigDecimal quantityTaken,
        BigDecimal unitCost
) {
}