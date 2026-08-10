package org.group1.coffeeshopapi.staff.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRequest {
    private String fullName;
    private String email;
    private String username;
    private String password;
    private String phoneNumber;
    private String role;
    private String status;
}