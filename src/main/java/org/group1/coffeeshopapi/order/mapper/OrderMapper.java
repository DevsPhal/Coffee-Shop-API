package org.group1.coffeeshopapi.order.mapper;

import org.group1.coffeeshopapi.order.dto.response.OrderItemExtraResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.order.entity.OrderItemExtra;
import org.group1.coffeeshopapi.product.service.ProductVariantPolicy;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "handledById", source = "order.handledBy")
    @Mapping(target = "handledByName", source = "handledByActor.name")
    @Mapping(target = "handledByRole", source = "handledByActor.role")
    @Mapping(target = "customerId", source = "order.customer.id")
    @Mapping(target = "customerName", source = "order.customer.fullName")
    @Mapping(target = "tableId", source = "order.diningTable.id")
    @Mapping(target = "distanceMeters", source = "distanceMeters")
    OrderResponse toResponse(Order order, ActorSummary handledByActor, BigDecimal distanceMeters);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productNameKh", source = "productNameKh")
    @Mapping(target = "variantName", source = "variant.name")
    @Mapping(target = "variantId", expression = "java(chosenSizeId(item))")
    @Mapping(target = "productImageUrl", source = "product.imageUrl")
    OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "extraId", source = "extra.id")
    @Mapping(target = "name", source = "extraName")
    @Mapping(target = "price", source = "extraPrice")
    @Mapping(target = "imageUrl", source = "extra.imageUrl")
    OrderItemExtraResponse toItemExtraResponse(OrderItemExtra extra);

    /** The size the customer picked, so "Reorder" can repeat it; null for products without a size choice. */
    default UUID chosenSizeId(OrderItem item) {
        if (item.getVariant() == null || item.getProduct() == null
                || !ProductVariantPolicy.allowsSizeChoice(item.getProduct())) {
            return null;
        }
        return item.getVariant().getId();
    }
}
