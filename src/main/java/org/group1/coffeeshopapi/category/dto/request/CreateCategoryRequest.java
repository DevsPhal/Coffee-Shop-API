package org.group1.coffeeshopapi.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        String description,

        // Which customizations products in this category accept. Leave null for an internal
        // category customers never order from directly.
        CategoryGroup categoryGroup
) {
}
