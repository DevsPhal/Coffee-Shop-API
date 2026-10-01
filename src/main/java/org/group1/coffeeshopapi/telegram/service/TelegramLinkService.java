package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.TelegramContact;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;

import java.util.Optional;
import java.util.UUID;

public interface TelegramLinkService {
    TelegramLinkCodeResponse generateLinkCode(UUID userId);

    String resolveLinkCode(String code, Long chatId);

    String verifyPendingContact(Long chatId, TelegramContact contact, Long senderUserId);

    String unlink(Long chatId);

    Optional<String> linkedUserName(Long chatId);
}
