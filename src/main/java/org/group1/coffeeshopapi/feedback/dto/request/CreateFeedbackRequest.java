package org.group1.coffeeshopapi.feedback.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.FeedbackTopic;

public record CreateFeedbackRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 120, message = "Full name must not exceed 120 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        String phoneNumber,

        @NotNull(message = "Topic is required")
        FeedbackTopic topic,

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message must not exceed 2000 characters")
        String message
) {
}
