package org.group1.coffeeshopapi.inventory.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryResponse {

    private UUID id;

    private UUID productId;

    private String productCode;

    private String productName;

    private Integer quantityOnHand;

    private Integer lowStockThreshold;

    private Boolean lowStock;

    private LocalDateTime lastRestockedAt;

    private LocalDateTime updatedAt;
}
