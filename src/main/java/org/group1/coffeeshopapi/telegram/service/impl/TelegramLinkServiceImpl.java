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
            throw new ResourceNotFoundException("This code is invalid or has expired. Please generate a new one.");
        }
        redisTemplate.delete(key);

        // Role-agnostic on purpose: this same code path now links a chat for a Customer
        // (self-registration/profile linking) or an Admin/Barista invited via Telegram (see
        // StaffServiceImpl#createViaTelegram) — findById/save on UserRepository dispatch to the
        // right physical table either way (TABLE_PER_CLASS).
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Account no longer exists"));

        if (user.getStatus() == UserStatus.PENDING_VERIFICATION && user.getRegisterType() == RegisterType.TELEGRAM) {
            // A staff account invited via Telegram (StaffServiceImpl#createViaTelegram) — a
            // customer never lands here PENDING_VERIFICATION, since generating a link code
            // (UserController) requires already being authenticated, which a PENDING_VERIFICATION
            // account can't do (CustomUserDetails#isEnabled). Don't link/activate yet: stash which
            // account this chat is verifying for and ask it to share its contact — verifyPendingContact
            // finishes the job once that arrives.
            redisTemplate.opsForValue().set(
                    RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + chatId,
                    user.getId().toString(),
                    Duration.ofSeconds(properties.getLinkCodeTtlSeconds()));
            telegramApiClient.sendContactRequest(chatId,
                    "👋 Hi " + user.getFullName() + "! To activate your account, please confirm it's really you "
                            + "by sharing your phone number below.");
            // Already replied above — nothing further for the caller to send.
            return null;
        }

        Optional<User> currentlyLinked = userRepository.findByTelegramChatId(chatId.toString());
        if (currentlyLinked.map(User::getId).filter(id -> id.equals(user.getId())).isPresent()) {
            // Re-sending a code for the account this chat is already linked to — nothing to do.
            return "✅ You're already linked as " + TelegramFormat.escape(user.getFullName()) + ".";
        }

        claimChat(user, currentlyLinked, chatId);

        return "✅ <b>Linked!</b> Welcome, " + TelegramFormat.escape(user.getFullName()) + ".\n\n"
                + "You'll get your order receipts, new event alerts, and reminders here from now on.";
    }

    @Override
    @Transactional
    public String verifyPendingContact(Long chatId, TelegramContact contact, Long senderUserId) {
        String key = RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + chatId;
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null) {
            return "I wasn't expecting a phone number from you right now. If you're finishing an invite, "
                    + "send /start &lt;code&gt; with your invite code first.";
        }

        // A request_contact button only ever shares the tapper's own card, but nothing stops
        // someone from forwarding a different contact card into this chat instead — reject that
        // rather than let it spoof a phone-number match. Key deliberately left in place so they
        // can immediately retry by tapping the button themselves.
        if (contact.userId() == null || !contact.userId().equals(senderUserId)) {
            return "Please share your own phone number using the button below, not someone else's contact.";
        }
        redisTemplate.delete(key);

        User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (user == null || user.getStatus() != UserStatus.PENDING_VERIFICATION
                || user.getRegisterType() != RegisterType.TELEGRAM) {
            return "This invite is no longer valid. Please ask your admin to resend it.";
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
                + "Open the app and log in with the Telegram button to get started.";
    }

    // Shared by both link paths: hands this chat id over to `user`, first freeing it from whoever
    // (if anyone) currently holds it.
    private void claimChat(User user, Optional<User> currentlyLinked, Long chatId) {
        currentlyLinked.ifPresent(existing -> {
            existing.setTelegramChatId(null);
            // Flush immediately: this chat id is still unique-constrained, so the old
            // owner's row must actually clear in the DB before the new owner's row below
            // claims it — otherwise Hibernate may flush both UPDATEs in the wrong order
            // and both rows briefly hold the same chat id, tripping the constraint.
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
                .orElseThrow(() -> new ResourceNotFoundException("This chat is not linked to any account"));
        user.setTelegramChatId(null);
        userRepository.save(user);
        authUserSyncService.sync(user);
        return "👋 Your account has been unlinked. You can still browse with /menu, /categories, /discounts, "
                + "/events and /rate — send /start <code> any time to link again.";
    }

    @Override
    public Optional<String> linkedUserName(Long chatId) {
        return userRepository.findByTelegramChatId(chatId.toString()).map(User::getFullName);
    }
}
