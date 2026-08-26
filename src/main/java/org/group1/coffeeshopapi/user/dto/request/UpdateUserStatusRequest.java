package org.group1.coffeeshopapi.user.dto.request;

import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.UserStatus;

public record UpdateUserStatusRequest(
        @NotNull(message = "Status is required")
        UserStatus status
) {
}
