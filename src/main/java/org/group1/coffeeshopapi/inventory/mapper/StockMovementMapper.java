package org.group1.coffeeshopapi.inventory.mapper;

import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
import org.group1.coffeeshopapi.inventory.entity.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    StockMovementResponse toResponse(StockMovement movement);
}