package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.Size;

public record AnswerStaffCallRequest(
        @Size(max = 200, message = "Reply must not exceed 200 characters")
        String reply
) {
}
