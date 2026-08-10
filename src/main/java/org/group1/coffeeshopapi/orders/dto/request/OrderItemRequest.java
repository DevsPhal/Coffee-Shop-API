package org.group1.coffeeshopapi.orders.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {
    private UUID id;
    private UUID productId;
    private String productName;
    private int quantity;
    private double price;
}
