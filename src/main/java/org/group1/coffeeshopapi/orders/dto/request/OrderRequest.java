package org.group1.coffeeshopapi.orders.dto.request;

import java.util.List;

import org.group1.coffeeshopapi.common.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    private String orderNumber;
    private String customerName;
    private String orderType;
    private OrderStatus status;
    private List<OrderItemRequest> items;
}
