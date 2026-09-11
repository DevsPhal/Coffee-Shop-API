package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.TelegramContact;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Role-agnostic Telegram chat linking — {@code telegramChatId} lives on the shared {@code User}
 * entity, so a chat can be linked to a Customer (order/receipt push, self-registration/profile
 * linking) or an Admin/Barista invited via Telegram (see StaffServiceImpl#createViaTelegram)
 * alike. Linking a still-{@code PENDING_VERIFICATION} staff invite doesn't activate it right
 * away — it instead prompts the chat to share its phone number, which {@link #verifyPendingContact}
 * checks against the number the admin entered before activating — see {@link #resolveLinkCode}.
 */
public interface TelegramLinkService {
    TelegramLinkCodeResponse generateLinkCode(UUID userId);

    // Returns null when the reply has already been sent directly (e.g. the contact-request
    // keyboard for a pending staff invite) rather than as a plain/HTML text reply — callers must
    // treat a null return as "already handled", not send anything themselves.
    String resolveLinkCode(String code, Long chatId);

    /**
     * Completes a staff invite once the invitee taps the "share phone number" button prompted by
     * {@link #resolveLinkCode}. Activates the account and links the chat only if {@code
     * senderUserId} actually owns {@code contact} (a request_contact button only ever shares the
     * tapper's own card, but a manually forwarded contact could spoof someone else's number) AND
     * its phone number matches the one their admin entered at invite time (see {@code
     * PhoneNumberUtil#matches}) — otherwise the account stays PENDING_VERIFICATION so the admin
     * can resend the invite (see StaffService#resendTelegramInvite).
     */
    String verifyPendingContact(Long chatId, TelegramContact contact, Long senderUserId);

    String unlink(Long chatId);

    // The linked account's display name, if this chat is currently linked to anyone — lets
    // /start (and anything else) branch on "already linked" vs. "not linked yet" without pulling
    // a User entity into the command layer.
    Optional<String> linkedUserName(Long chatId);
}