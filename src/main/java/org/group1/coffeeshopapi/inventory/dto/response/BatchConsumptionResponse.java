package org.group1.coffeeshopapi.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BatchConsumptionResponse(
        UUID batchId,
        LocalDateTime batchReceivedAt,
        BigDecimal quantityTaken,
        BigDecimal unitCost
) {
}
