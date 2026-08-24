package org.group1.coffeeshopapi.order.mapper;

import org.group1.coffeeshopapi.order.dto.response.OrderItemResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // barista/customer are real relations on Order (see its javadoc), so their id/name/role are
    // read straight off the associated row — MapStruct null-checks each nested path automatically,
    // so an order with no barista (or no customer) simply maps those fields to null.
    @Mapping(target = "baristaId", source = "barista.id")
    @Mapping(target = "baristaName", source = "barista.fullName")
    @Mapping(target = "baristaRole", source = "barista.role")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.fullName")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toItemResponse(OrderItem item);
}
