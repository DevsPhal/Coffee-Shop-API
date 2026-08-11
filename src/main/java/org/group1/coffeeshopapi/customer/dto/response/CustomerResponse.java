package org.group1.coffeeshopapi.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private long totalOrders;
    private double totalSpent;
    private String createdAt;
    private String status;
    private boolean locked;
}