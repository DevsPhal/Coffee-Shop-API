package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String sizeOptionName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType
) {
}
