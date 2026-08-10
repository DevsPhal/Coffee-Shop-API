package org.group1.coffeeshopapi.orders.dto.response;

import java.util.List;
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
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String customerName;
    private String orderType;
    private String status;
    private double totalAmount;
    private List<OrderItemResponse> items;
    private String createdAt;
    private String updatedAt;
}
