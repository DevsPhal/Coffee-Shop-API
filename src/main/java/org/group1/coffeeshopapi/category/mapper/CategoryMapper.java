package org.group1.coffeeshopapi.category.mapper;

import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}
