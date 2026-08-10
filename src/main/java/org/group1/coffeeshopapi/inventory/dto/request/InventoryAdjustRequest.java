package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryAdjustRequest {

    @NotNull(message = "Quantity change is required")
    private Integer quantityChange;

    private String reason;
}
