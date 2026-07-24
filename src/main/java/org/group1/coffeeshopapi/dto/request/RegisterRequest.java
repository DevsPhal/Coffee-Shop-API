package org.group1.coffeeshopapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payload")
public class RegisterRequest {
   @NotBlank(message = "Username is required")
   @Size(min = 4, max = 20)
   @Schema(example = "your name")
   private String fullName;

   @NotBlank(message = "Email is required")
   @Email(message = "Invalid email format")
   @Schema(example = "you@gmail.com")
   private String email;

   @NotBlank(message = "Password is required")
   @Size(min = 8, message = "Password must be at least 8 character")
   @Schema(example = "Qwert12!@")
   private String password;
}
