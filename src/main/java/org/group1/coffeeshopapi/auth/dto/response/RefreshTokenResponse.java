package org.group1.coffeeshopapi.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshTokenResponse {
    private String accessToken;
}
