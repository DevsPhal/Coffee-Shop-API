package org.group1.coffeeshopapi.contact;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContactMessageResponse(UUID id, String fullName, String email, String phone,
        String topic, String message, ContactMessage.Status status, LocalDateTime createdAt) {
    public static ContactMessageResponse of(ContactMessage message) {
        return new ContactMessageResponse(message.getId(), message.getFullName(), message.getEmail(),
                message.getPhone(), message.getTopic(), message.getMessage(), message.getStatus(), message.getCreatedAt());
    }
}
