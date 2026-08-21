package org.group1.coffeeshopapi.finance.mapper;

import org.group1.coffeeshopapi.finance.dto.response.ExpenseResponse;
import org.group1.coffeeshopapi.finance.entity.Expense;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    ExpenseResponse toResponse(Expense expense);
}
