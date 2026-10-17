package org.group1.coffeeshopapi.telegram.dto;

/**
 * A user's current Telegram profile photo as the Bot API identifies it. {@code fileUniqueId} stays the same for the
 * same picture, so it tells whether the photo changed without downloading it again.
 */
public record TelegramProfilePhoto(String fileId, String fileUniqueId) {
}
