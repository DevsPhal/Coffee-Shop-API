package org.group1.coffeeshopapi.finance.mapper;

import org.group1.coffeeshopapi.finance.dto.response.ExpenseResponse;
import org.group1.coffeeshopapi.finance.entity.Expense;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    @Mapping(target = "id", source = "expense.id")
    @Mapping(target = "recordedByName", source = "recordedByActor.name")
    @Mapping(target = "recordedByRole", source = "recordedByActor.role")
    ExpenseResponse toResponse(Expense expense, ActorSummary recordedByActor);
}
