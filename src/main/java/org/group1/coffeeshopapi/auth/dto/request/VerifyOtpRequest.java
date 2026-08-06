package org.group1.coffeeshopapi.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema
public class VerifyOtpRequest {
    @NotBlank @Email
    @Schema(example = "you@gmail.com")
    private String email;

    @NotBlank
    private String otp;
}
