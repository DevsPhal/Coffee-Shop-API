package org.group1.coffeeshopapi.cart.dto.response;

import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productNameKh,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        UUID variantId,
        VariantLabel variantName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType,
        // Extras (e.g. Pearl) added to this item — unitPrice/subtotal already include their price.
        List<CartItemExtraResponse> extras
) {
}
