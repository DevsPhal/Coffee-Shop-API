package org.group1.coffeeshopapi.category.mapper;

import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.dto.response.CustomerCategoryResponse;
import org.group1.coffeeshopapi.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", source = "category.id")
    @Mapping(target = "name", source = "category.name")
    @Mapping(target = "createdBy", source = "category.createdByAdmin.id")
    @Mapping(target = "createdByName", source = "category.createdByAdmin.fullName")
    @Mapping(target = "createdByRole", source = "category.createdByAdmin.role")
    @Mapping(target = "updatedBy", source = "category.updatedByAdmin.id")
    @Mapping(target = "updatedByName", source = "category.updatedByAdmin.fullName")
    @Mapping(target = "updatedByRole", source = "category.updatedByAdmin.role")
    CategoryResponse toResponse(Category category);

    // Strips staff audit identities before a category reaches a customer.
    CustomerCategoryResponse toCustomerResponse(CategoryResponse response);
}
