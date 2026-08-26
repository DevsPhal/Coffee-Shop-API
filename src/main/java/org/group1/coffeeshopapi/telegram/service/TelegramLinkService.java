package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Telegram linking is currently exposed for customers only (used to push order/receipt info
 * to customers). {@code telegramChatId} lives on the shared {@code User} entity, so admin and
 * barista accounts have the column too, but this service doesn't wire up a link flow for them
 * yet.
 */
public interface TelegramLinkService {
    TelegramLinkCodeResponse generateLinkCode(UUID customerId);
    String resolveLinkCode(String code, Long chatId);
    String unlink(Long chatId);

    // The linked customer's display name, if this chat is currently linked to anyone — lets
    // /start (and anything else) branch on "already linked" vs. "not linked yet" without pulling
    // a Customer entity into the command layer.
    Optional<String> linkedCustomerName(Long chatId);
}