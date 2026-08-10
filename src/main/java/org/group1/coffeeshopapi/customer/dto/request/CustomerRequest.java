package org.group1.coffeeshopapi.customer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {
    private String firstName;
    private String familyName;
    private String username;
    private String email;
    private String password;
    private String phone;
    private String address;
}