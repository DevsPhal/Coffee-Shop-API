package org.group1.coffeeshopapi.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
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