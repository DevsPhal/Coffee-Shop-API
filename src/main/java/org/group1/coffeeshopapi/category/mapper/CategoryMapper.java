package org.group1.coffeeshopapi.category.mapper;

import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", source = "category.id")
    @Mapping(target = "name", source = "category.name")
    @Mapping(target = "createdByName", source = "createdByActor.name")
    @Mapping(target = "createdByRole", source = "createdByActor.role")
    @Mapping(target = "updatedByName", source = "updatedByActor.name")
    @Mapping(target = "updatedByRole", source = "updatedByActor.role")
    CategoryResponse toResponse(Category category, ActorSummary createdByActor, ActorSummary updatedByActor);
}
