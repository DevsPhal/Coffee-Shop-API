package org.group1.coffeeshopapi.category.dto.request;

import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.Status;

public record UpdateCategoryRequest(
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,
        Status status,

        CategoryGroup categoryGroup
) {
}
