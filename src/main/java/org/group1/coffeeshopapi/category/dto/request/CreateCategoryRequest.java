package org.group1.coffeeshopapi.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        // Which customizations products in this category accept. Leave null for an internal
        // category customers never order from directly.
        CategoryGroup categoryGroup
) {
}
