package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;

import java.util.UUID;

/**
 * Telegram linking is a customer-only feature (used to push order/receipt info to customers) —
 * admin and barista accounts have no Telegram concept.
 */
public interface TelegramLinkService {
    TelegramLinkCodeResponse generateLinkCode(UUID customerId);
    String resolveLinkCode(String code, Long chatId);
    String unlink(Long chatId);
}