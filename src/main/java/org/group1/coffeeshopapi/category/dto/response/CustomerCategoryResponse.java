package org.group1.coffeeshopapi.category.dto.response;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;

import java.util.UUID;

// Customer-facing category view — leaves out staff audit fields.
public record CustomerCategoryResponse(
        UUID id,
        String name,
        String description,
        CategoryGroup categoryGroup
) {
}
