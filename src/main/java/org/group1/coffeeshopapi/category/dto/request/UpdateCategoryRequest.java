package org.group1.coffeeshopapi.category.dto.request;

import org.group1.coffeeshopapi.common.enums.Status;

public record UpdateCategoryRequest(
        String name,
        String description,
        Status status
) {
}