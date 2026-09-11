package org.group1.coffeeshopapi.inventory.mapper;

import org.group1.coffeeshopapi.inventory.dto.response.StockExpenseResponse;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockExpenseMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "stockMovementId", source = "stockMovement.id")
    StockExpenseResponse toResponse(StockExpense stockExpense);
}
