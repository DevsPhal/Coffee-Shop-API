package org.group1.coffeeshopapi.orders.dto.request;

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
public class OrderItemRequest {
    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private double price;
}
