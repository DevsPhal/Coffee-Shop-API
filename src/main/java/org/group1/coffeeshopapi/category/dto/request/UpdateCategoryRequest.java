package org.group1.coffeeshopapi.category.dto.request;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.Status;

public record UpdateCategoryRequest(
        String name,
        String description,
        Status status,

        // See CreateCategoryRequest.categoryGroup — null here means "leave unchanged", same as
        // every other field on this request.
        CategoryGroup categoryGroup
) {
}