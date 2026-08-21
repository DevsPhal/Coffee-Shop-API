package org.group1.coffeeshopapi.category.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        String description
) {
}
