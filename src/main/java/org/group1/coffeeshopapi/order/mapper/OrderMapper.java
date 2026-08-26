package org.group1.coffeeshopapi.order.mapper;

import org.group1.coffeeshopapi.order.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // customer is a real relation on Order (see its javadoc), so its id/name are read straight
    // off the associated row — MapStruct null-checks the nested path automatically, so an order
    // with no customer simply maps those fields to null. handledBy is a plain audit id instead,
    // so its display name/role come from the resolved ActorSummary passed in alongside the order.
    @Mapping(target = "id", source = "order.id")
    @Mapping(target = "handledById", source = "order.handledBy")
    @Mapping(target = "handledByName", source = "handledByActor.name")
    @Mapping(target = "handledByRole", source = "handledByActor.role")
    @Mapping(target = "customerId", source = "order.customer.id")
    @Mapping(target = "customerName", source = "order.customer.fullName")
    OrderResponse toResponse(Order order, ActorSummary handledByActor);

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toItemResponse(OrderItem item);
}
