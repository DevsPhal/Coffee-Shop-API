package org.group1.coffeeshopapi.admin.mapper;

import org.group1.coffeeshopapi.admin.entity.Inventory;
import org.group1.coffeeshopapi.admin.entity.Product;
import org.group1.coffeeshopapi.admin.dto.request.InventoryPatchRequest;
import org.group1.coffeeshopapi.admin.dto.request.InventoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.InventoryResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public Inventory toEntity(InventoryRequest request, Product product) {
        return Inventory.builder()
                .product(product)
                .quantity(request.getQuantity())
                .minimumStock(request.getMinimumStock())
                .build();
    }

    public void updateEntity(Inventory inventory, InventoryRequest request) {
        inventory.setQuantity(request.getQuantity());
        inventory.setMinimumStock(request.getMinimumStock());
    }

    public void patch(Inventory inventory, InventoryPatchRequest request) {
        if (request.getQuantity() != null) {
            inventory.setQuantity(request.getQuantity());
        }
        if (request.getMinimumStock() != null) {
            inventory.setMinimumStock(request.getMinimumStock());
        }
    }

    public InventoryResponse toResponse(Inventory inventory) {
        boolean lowStock = inventory.getMinimumStock() != null
                && inventory.getQuantity() <= inventory.getMinimumStock();
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .quantity(inventory.getQuantity())
                .minimumStock(inventory.getMinimumStock())
                .inStock(inventory.getInStock())
                .lowStock(lowStock)
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}