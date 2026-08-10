package org.group1.coffeeshopapi.orders.dto.response;

import java.util.List;
import java.util.UUID;
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
    private UUID id;
    private String orderNumber;
    private UUID userId;
    private String customerName;
    private String orderType;
    private String status;
    private double totalAmount;
    private List<OrderItemResponse> items;
    private String createdAt;
    private String updatedAt;
}
