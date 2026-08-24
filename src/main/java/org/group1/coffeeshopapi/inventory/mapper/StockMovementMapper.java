package org.group1.coffeeshopapi.inventory.mapper;

import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
import org.group1.coffeeshopapi.inventory.entity.StockMovement;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "id", source = "movement.id")
    @Mapping(target = "productId", source = "movement.product.id")
    @Mapping(target = "productName", source = "movement.product.name")
    @Mapping(target = "performedByName", source = "performedByActor.name")
    @Mapping(target = "performedByRole", source = "performedByActor.role")
    StockMovementResponse toResponse(StockMovement movement, ActorSummary performedByActor);
}