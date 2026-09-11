package org.group1.coffeeshopapi.category.mapper;

import org.group1.coffeeshopapi.category.dto.response.CategoryResponse;
import org.group1.coffeeshopapi.category.dto.response.CustomerCategoryResponse;
import org.group1.coffeeshopapi.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // createdByAdmin/updatedByAdmin are null both for pre-existing rows and for a change made by
    // the Super Admin (see Category's javadoc) — MapStruct null-checks the nested path
    // automatically, so createdByName/createdByRole simply come out null too in that case.
    @Mapping(target = "id", source = "category.id")
    @Mapping(target = "name", source = "category.name")
    @Mapping(target = "createdBy", source = "category.createdByAdmin.id")
    @Mapping(target = "createdByName", source = "category.createdByAdmin.fullName")
    @Mapping(target = "createdByRole", source = "category.createdByAdmin.role")
    @Mapping(target = "updatedBy", source = "category.updatedByAdmin.id")
    @Mapping(target = "updatedByName", source = "category.updatedByAdmin.fullName")
    @Mapping(target = "updatedByRole", source = "category.updatedByAdmin.role")
    CategoryResponse toResponse(Category category);

    // Strips staff audit identities before a category reaches a customer — see
    // CustomerCategoryResponse's javadoc.
    CustomerCategoryResponse toCustomerResponse(CategoryResponse response);
}
