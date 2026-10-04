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
        String productNameKh,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        VariantLabel variantName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType,
        List<OrderItemExtraResponse> extras,
        UUID variantId,
        String productImageUrl
) {
}
