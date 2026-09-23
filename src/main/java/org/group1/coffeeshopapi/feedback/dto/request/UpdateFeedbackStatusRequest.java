package org.group1.coffeeshopapi.feedback.dto.request;

import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.FeedbackStatus;

public record UpdateFeedbackStatusRequest(
        @NotNull(message = "Status is required")
        FeedbackStatus status
) {
}
