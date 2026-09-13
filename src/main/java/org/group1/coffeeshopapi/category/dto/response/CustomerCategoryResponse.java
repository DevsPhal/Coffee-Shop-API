package org.group1.coffeeshopapi.category.dto.response;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;

import java.util.UUID;

/**
 * Customer-facing projection of {@link CategoryResponse} — deliberately excludes staff audit
 * identities, same as {@code CustomerProductResponse} does for products. categoryGroup is kept
 * (unlike the audit fields) since the client needs it to know which order-item customizations
 * (size/sugar/ice/milk) to offer for products in this category — see ProductVariantPolicy.
 */
public record CustomerCategoryResponse(
        UUID id,
        String name,
        String description,
        CategoryGroup categoryGroup
) {
}
