package org.group1.coffeeshopapi.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        String description,

        // Which cart/order customizations products in this category accept — see
        // ProductVariantPolicy. Optional: leave null for an internal, non-menu category (e.g.
        // stock-in raw materials) that customers never order directly.
        CategoryGroup categoryGroup
) {
}
