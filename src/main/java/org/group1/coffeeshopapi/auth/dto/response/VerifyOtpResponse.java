package org.group1.coffeeshopapi.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifyOtpResponse {
    private boolean verified;
    private String email;
}
