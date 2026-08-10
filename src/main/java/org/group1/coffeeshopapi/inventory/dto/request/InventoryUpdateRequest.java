package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryUpdateRequest {

    @PositiveOrZero(message = "Quantity on hand cannot be negative")
    private Integer quantityOnHand;

    @PositiveOrZero(message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;
}
