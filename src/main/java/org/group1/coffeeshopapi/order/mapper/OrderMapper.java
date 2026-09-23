package org.group1.coffeeshopapi.order.mapper;

import org.group1.coffeeshopapi.order.dto.response.OrderItemExtraResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.order.entity.OrderItemExtra;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // handledBy is a plain audit id, so its display name/role come from the ActorSummary
    // resolved separately and passed in.
    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "handledById", source = "order.handledBy")
    @Mapping(target = "handledByName", source = "handledByActor.name")
    @Mapping(target = "handledByRole", source = "handledByActor.role")
    @Mapping(target = "customerId", source = "order.customer.id")
    @Mapping(target = "customerName", source = "order.customer.fullName")
    @Mapping(target = "distanceMeters", source = "distanceMeters")
    OrderResponse toResponse(Order order, ActorSummary handledByActor, BigDecimal distanceMeters);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productNameKh", source = "productNameKh")
    @Mapping(target = "variantName", source = "variant.name")
    OrderItemResponse toItemResponse(OrderItem item);

    @Mapping(target = "extraId", source = "extra.id")
    @Mapping(target = "name", source = "extraName")
    @Mapping(target = "price", source = "extraPrice")
    @Mapping(target = "imageUrl", source = "extra.imageUrl")
    OrderItemExtraResponse toItemExtraResponse(OrderItemExtra extra);
}
