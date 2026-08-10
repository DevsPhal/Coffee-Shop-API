package org.group1.coffeeshopapi.orders.mapper;

import java.util.List;

import org.group1.coffeeshopapi.orders.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.orders.dto.request.OrderRequest;
import org.group1.coffeeshopapi.orders.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.orders.dto.response.OrderResponse;
import org.group1.coffeeshopapi.orders.entity.Order;
import org.group1.coffeeshopapi.orders.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", source = "items")
    @Mapping(target = "status", source = "status", defaultValue = "PENDING")
    @Mapping(target = "totalAmount", ignore = true)
    Order toEntity(OrderRequest request);

    @Mapping(target = "items", source = "items")
    @Mapping(target = "createdAt", expression = "java(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)")
    @Mapping(target = "updatedAt", expression = "java(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null)")
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderResponse> toResponses(List<Order> orders);

    List<OrderItemResponse> toItemResponses(List<OrderItem> items);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toItemEntity(OrderItemRequest itemRequest);

    List<OrderItem> toItemEntities(List<OrderItemRequest> items);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    void updateEntity(OrderRequest request, @MappingTarget Order order);
}
