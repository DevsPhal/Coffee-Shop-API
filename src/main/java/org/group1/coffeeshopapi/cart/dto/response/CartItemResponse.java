package org.group1.coffeeshopapi.cart.dto.response;

import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        UUID sizeOptionId,
        String sizeOptionName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType
) {
}
