package org.group1.coffeeshopapi.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterResponse {
    private Long userId;
    private String email;
    private boolean verificationRequired;
    private String devOtp;
}
