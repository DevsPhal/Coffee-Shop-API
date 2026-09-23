package org.group1.coffeeshopapi.realtime.dto;

import org.group1.coffeeshopapi.realtime.ChangeType;
import org.group1.coffeeshopapi.realtime.ResourceType;

import java.time.LocalDateTime;
import java.util.UUID;

// A "this changed, refetch it" signal. Carries no data, so what a client shows always comes
// from the normal REST endpoints and their permission checks.
public record ResourceChangeMessage(ResourceType resource, UUID id, ChangeType change, LocalDateTime sentAt) {
}
