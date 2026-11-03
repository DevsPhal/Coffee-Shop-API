package org.group1.coffeeshopapi.inventory.dto.request;

import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;

import java.time.LocalDate;
import java.util.UUID;

// Inventory history filters; every one is optional and the dates are inclusive.
public record MovementSearch(UUID productId, StockMovementType type, StockMovementReason reason, LocalDate from,
        LocalDate to) {
}
