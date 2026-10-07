package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.util.Map;
import java.util.Set;

public record SkuSuggestionResponse(
        String sku,
        String prefix,
        String categoryName,
        CategoryGroup categoryGroup,
        Set<VariantLabel> allowedVariants,
        Map<VariantLabel, String> variantSkus
) {
}
