package org.group1.coffeeshopapi.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    @Email
    private String email;
    private String otp;

    @Size(min = 8, message = "Password must be 8 char")
    private String newPassword;
}
