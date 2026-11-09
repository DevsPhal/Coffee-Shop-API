package org.group1.coffeeshopapi.category.dto.request;

import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.Status;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record UpdateCategoryRequest(
        @Pattern(regexp = ValidationPatterns.NOT_BLANK_REGEX, message = "Category name can't be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,
        Status status,

        CategoryGroup categoryGroup
) {
}
