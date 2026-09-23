package org.group1.coffeeshopapi.feedback.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.FeedbackTopic;

public record CreateFeedbackRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        String phoneNumber,

        @NotNull(message = "Topic is required")
        FeedbackTopic topic,

        @NotBlank(message = "Message is required")
        String message
) {
}
