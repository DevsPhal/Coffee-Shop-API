package org.group1.coffeeshopapi.staff.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String status;
    private String joinDate;
}