package org.group1.coffeeshopapi.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
   private String fullName;
   private String email;
   private String password;
}
