package org.group1.coffeeshopapi.feedback.dto.response;

import org.group1.coffeeshopapi.common.enums.FeedbackStatus;
import org.group1.coffeeshopapi.common.enums.FeedbackTopic;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        UUID customerId,
        String customerName,
        String fullName,
        String email,
        String phoneNumber,
        FeedbackTopic topic,
        String message,
        FeedbackStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
