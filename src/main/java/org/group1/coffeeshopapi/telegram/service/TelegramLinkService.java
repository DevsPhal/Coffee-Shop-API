package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.TelegramContact;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;

import java.util.Optional;
import java.util.UUID;

// Links a Telegram chat to a Customer, Admin, or Barista account. A staff account invited via
// Telegram isn't activated on link alone — it must also confirm its phone number first, via
// verifyPendingContact.
public interface TelegramLinkService {
    TelegramLinkCodeResponse generateLinkCode(UUID userId);

    // Null means the reply was already sent directly — callers should send nothing themselves.
    String resolveLinkCode(String code, Long chatId);

    // Completes a staff invite: activates the account and links the chat, but only if the shared
    // contact belongs to whoever sent it and its phone number matches the invite.
    String verifyPendingContact(Long chatId, TelegramContact contact, Long senderUserId);

    String unlink(Long chatId);

    // The linked account's display name, if this chat is linked to anyone.
    Optional<String> linkedUserName(Long chatId);
}