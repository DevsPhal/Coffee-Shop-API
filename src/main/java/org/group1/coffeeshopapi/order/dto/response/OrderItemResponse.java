package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        VariantLabel variantName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType,
        // Extras (e.g. Pearl) added to this line — unitPrice/subtotal already include their price.
        List<OrderItemExtraResponse> extras
) {
}
