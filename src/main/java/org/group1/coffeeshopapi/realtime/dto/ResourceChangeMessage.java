package org.group1.coffeeshopapi.realtime.dto;

import org.group1.coffeeshopapi.realtime.ChangeType;
import org.group1.coffeeshopapi.realtime.ResourceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResourceChangeMessage(ResourceType resource, UUID id, ChangeType change, LocalDateTime sentAt) {
}
