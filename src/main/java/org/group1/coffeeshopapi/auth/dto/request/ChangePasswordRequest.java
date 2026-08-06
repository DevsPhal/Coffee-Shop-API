package org.group1.coffeeshopapi.auth.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    private String oldPassword;
    @Size(min = 8)
    private String newPassword;
}
