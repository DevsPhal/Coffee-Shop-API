package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.util.PhoneNumberUtil;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.TelegramContact;
import org.group1.coffeeshopapi.telegram.dto.TelegramLinkCodeResponse;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.telegram.util.TelegramCodeGenerator;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramLinkServiceImpl implements TelegramLinkService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final TelegramProperties properties;
    private final AuthUserSyncService authUserSyncService;
    private final TelegramApiClient telegramApiClient;

    @Override
    public TelegramLinkCodeResponse generateLinkCode(UUID userId) {
        String code = TelegramCodeGenerator.generate();
        redisTemplate.opsForValue().set(
                RedisKeys.TELEGRAM_LINK_CODE_PREFIX + code,
                userId.toString(),
                Duration.ofSeconds(properties.getLinkCodeTtlSeconds()));
        return new TelegramLinkCodeResponse(code, properties.getLinkCodeTtlSeconds(), properties.deepLink(code));
    }

    @Override
    @Transactional
    public String resolveLinkCode(String code, Long chatId) {
        String key = RedisKeys.TELEGRAM_LINK_CODE_PREFIX + code.toUpperCase();
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            throw new ResourceNotFoundException("❌ This code is invalid or has expired. Please generate a new one.");
        }
        redisTemplate.delete(key);

        // Works the same for a Customer, Admin, or Barista account.
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("❌ Account no longer exists."));

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION && user.getRegisterType() == RegisterType.TELEGRAM) {
            // A staff invite pending phone verification. Don't activate yet — ask for their
            // contact first; verifyPendingContact finishes the job.
            redisTemplate.opsForValue().set(
                    RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + chatId,
                    user.getId().toString(),
                    Duration.ofSeconds(properties.getLinkCodeTtlSeconds()));
            telegramApiClient.sendContactRequest(chatId,
                    "👋 Hi " + TelegramFormat.titleCase(user.getFullName()) + "! To activate your account, please "
                            + "confirm it's really you by sharing your phone number below.");
            // Already replied above.
            return null;
        }

        Optional<User> currentlyLinked = userRepository.findByTelegramChatId(chatId.toString());
        if (currentlyLinked.map(User::getId).filter(id -> id.equals(user.getId())).isPresent()) {
            // Already linked to this account — nothing to do.
            return "✅ <b>You're already linked</b> as "
                    + TelegramFormat.escape(TelegramFormat.titleCase(user.getFullName())) + ".";
        }

        claimChat(user, currentlyLinked, chatId);

        return "✅ <b>Linked!</b> Welcome, " + TelegramFormat.escape(TelegramFormat.titleCase(user.getFullName())) + ".\n\n"
                + "You'll get your order receipts, new event alerts, and reminders here from now on.";
    }

    @Override
    @Transactional
    public String verifyPendingContact(Long chatId, TelegramContact contact, Long senderUserId) {
        String key = RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + chatId;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return "ℹ️ I wasn't expecting a phone number from you right now. If you're finishing an invite, "
                    + "send /start &lt;code&gt; with your invite code first.";
        }

        // Reject a forwarded contact card that isn't the sender's own — the key stays in place so
        // they can retry with the button themselves.
        if (contact.userId() == null || !contact.userId().equals(senderUserId)) {
            return "⚠️ Please share your own phone number using the button below, not someone else's contact.";
        }
        redisTemplate.delete(key);

        User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (user == null || user.getStatus() != UserStatus.PENDING_VERIFICATION
                || user.getRegisterType() != RegisterType.TELEGRAM) {
            return "❌ This invite is no longer valid. Please ask your admin to resend it.";
        }

        if (!PhoneNumberUtil.matches(user.getPhoneNumber(), contact.phoneNumber())) {
            return "❌ That phone number doesn't match the one on your invite. Please ask your admin to resend "
                    + "the invite link, then try again using the Telegram account registered under your own "
                    + "phone number.";
        }

        Optional<User> currentlyLinked = userRepository.findByTelegramChatId(chatId.toString());
        claimChat(user, currentlyLinked, chatId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        authUserSyncService.sync(user);

        return "✅ <b>Verified!</b> Your phone number matches — your account is now active.\n\n"
                + "Sign in to the 590st Cafe admin dashboard with <b>Log in with Telegram</b> — "
                + "no email or password needed.";
    }

    // Hands this chat id to `user`, first freeing it from whoever currently holds it.
    private void claimChat(User user, Optional<User> currentlyLinked, Long chatId) {
        currentlyLinked.ifPresent(existing -> {
            existing.setTelegramChatId(null);
            // Flush now so the old owner clears before the new owner claims the same chat id
            // (it's unique-constrained).
            userRepository.saveAndFlush(existing);
            authUserSyncService.sync(existing);
        });

        user.setTelegramChatId(chatId.toString());
        userRepository.save(user);
        authUserSyncService.sync(user);
    }

    @Override
    @Transactional
    public String unlink(Long chatId) {
        User user = userRepository.findByTelegramChatId(chatId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("ℹ️ This chat is not linked to any account."));
        user.setTelegramChatId(null);
        userRepository.save(user);
        authUserSyncService.sync(user);
        return "✅ <b>Unlinked.</b> You can still browse with /menu, /categories, /discounts, "
                + "/events and /rate — send /start &lt;code&gt; any time to link again.";
    }

    @Override
    public Optional<String> linkedUserName(Long chatId) {
        return userRepository.findByTelegramChatId(chatId.toString()).map(User::getFullName);
    }
}
