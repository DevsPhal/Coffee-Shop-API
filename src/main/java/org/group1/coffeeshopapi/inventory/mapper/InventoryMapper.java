package org.group1.coffeeshopapi.inventory.mapper;

import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.productId", target = "productCode")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(target = "lowStock", expression = "java(inventory.getQuantityOnHand() != null "
            + "&& inventory.getLowStockThreshold() != null "
            + "&& inventory.getQuantityOnHand() <= inventory.getLowStockThreshold())")
    InventoryResponse toResponse(Inventory inventory);
}
