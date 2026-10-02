package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.StaffCallReason;

public record StaffCallRequest(
        @NotNull(message = "Please choose a reason for calling staff")
        StaffCallReason reason,
        @Size(max = 200, message = "Note must not exceed 200 characters")
        String note
) {
}
