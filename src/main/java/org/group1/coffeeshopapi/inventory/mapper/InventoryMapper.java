package org.group1.coffeeshopapi.inventory.mapper;

import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "unit", source = "product.sellUnit")
    @Mapping(target = "stockUnit", source = "product.stockUnit")
    @Mapping(target = "unitsPerStock", source = "product.unitsPerStock")
    @Mapping(target = "ingredient", expression = "java(inventory.getProduct().isIngredient())")
    InventoryResponse toResponse(Inventory inventory);
}
