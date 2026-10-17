package org.group1.coffeeshopapi.telegram.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.TelegramLoginStatus;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.TelegramCallbackQuery;
import org.group1.coffeeshopapi.telegram.dto.TelegramChat;
import org.group1.coffeeshopapi.telegram.dto.TelegramLoginChallenge;
import org.group1.coffeeshopapi.telegram.dto.TelegramLoginOutcome;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.TelegramUser;
import org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramLoginService;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramLoginServiceImpl implements TelegramLoginService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9_-]{22}");
    private static final String CONFIRM = "ok";
    private static final String CANCEL = "no";

    private static final String EXPIRED_MESSAGE = "⌛ This login link has expired. Go back to the 590st Cafe website "
            + "and tap <b>Log in with Telegram</b> again.";
    private static final String PRIVATE_CHAT_ONLY_MESSAGE =
            "🔒 For your security, open the login link in a private chat with me.";
    private static final String CLAIMED_ELSEWHERE_MESSAGE = "⚠️ This login link was already opened from a different "
            + "Telegram account. Go back to the website and start a new login.";
    private static final String USERNAME_REQUIRED_MESSAGE = "👤 Please set a username in Telegram (Settings → Username) "
            + "first, then open the login link again.";
    private static final String TOO_MANY_ATTEMPTS_MESSAGE = "⏳ Too many login attempts from this Telegram account. "
            + "Please wait a few minutes and try again.";
    private static final String ALREADY_ANSWERED_MESSAGE =
            "ℹ️ You've already answered this login request. Go back to your browser.";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final TelegramProperties properties;
    private final TelegramApiClient telegramApiClient;
    private final ObjectMapper objectMapper;

    @Override
    public TelegramLoginChallenge createChallenge() {
        if (!properties.isBotLoginConfigured()) {
            log.error("Telegram login was requested but TELEGRAM_BOT_TOKEN, TELEGRAM_BOT_USERNAME or "
                    + "TELEGRAM_WEBHOOK_SECRET is not set");
            throw new TelegramDeliveryException(TelegramDeliveryException.Reason.NOT_CONFIGURED,
                    "Telegram sign-in isn't available right now. Please log in with your email instead.");
        }

        String requestId = randomToken(16);
        String pollToken = randomToken(32);
        int ttlSeconds = properties.getLoginRequestTtlSeconds();
        ops().set(RedisKeys.telegramLoginKey(requestId), sha256(pollToken), Duration.ofSeconds(ttlSeconds));
        return new TelegramLoginChallenge(requestId, pollToken, properties.deepLink(START_PREFIX + requestId),
                properties.plainBotUsername(), ttlSeconds);
    }

    @Override
    public TelegramLoginOutcome checkChallenge(String requestId, String pollToken) {
        if (!isWellFormed(requestId) || pollToken == null) {
            return TelegramLoginOutcome.of(TelegramLoginStatus.EXPIRED);
        }
        String loginKey = RedisKeys.telegramLoginKey(requestId);
        String storedHash = ops().get(loginKey);
        if (storedHash == null || !MessageDigest.isEqual(storedHash.getBytes(StandardCharsets.UTF_8),
                sha256(pollToken).getBytes(StandardCharsets.UTF_8))) {
            return TelegramLoginOutcome.of(TelegramLoginStatus.EXPIRED);
        }

        String userKey = RedisKeys.telegramLoginUserKey(requestId);
        String decisionKey = RedisKeys.telegramLoginDecisionKey(requestId);
        String decision = ops().get(decisionKey);
        if (decision == null) {
            return TelegramLoginOutcome.of(ops().get(userKey) == null
                    ? TelegramLoginStatus.PENDING
                    : TelegramLoginStatus.AWAITING_CONFIRMATION);
        }

        String claimant = ops().get(userKey);
        boolean consumed = Boolean.TRUE.equals(redisTemplate.delete(loginKey));
        redisTemplate.delete(List.of(userKey, decisionKey));
        if (!consumed || claimant == null) {
            return TelegramLoginOutcome.of(TelegramLoginStatus.EXPIRED);
        }
        return TelegramLoginStatus.APPROVED.name().equals(decision)
                ? TelegramLoginOutcome.approved(readUser(claimant))
                : TelegramLoginOutcome.of(TelegramLoginStatus.REJECTED);
    }

    @Override
    public String handleStart(String requestId, TelegramMessage message) {
        TelegramChat chat = message.chat();
        TelegramUser from = message.from();
        if (chat == null || !"private".equals(chat.type()) || from == null || from.id() == null
                || !from.id().equals(chat.id())) {
            return PRIVATE_CHAT_ONLY_MESSAGE;
        }
        Duration remaining = isWellFormed(requestId) ? remainingTtl(requestId) : null;
        if (remaining == null) {
            return EXPIRED_MESSAGE;
        }

        String userKey = RedisKeys.telegramLoginUserKey(requestId);
        String claimant = ops().get(userKey);
        if (claimant != null) {
            return resumeClaimedRequest(requestId, readUser(claimant), from);
        }

        Optional<User> linkedAccount = userRepository.findByTelegramChatId(from.id().toString());
        if (linkedAccount.isEmpty() && (from.username() == null || from.username().isBlank())) {
            return USERNAME_REQUIRED_MESSAGE;
        }
        if (exceedsAttemptLimit(from.id())) {
            return TOO_MANY_ATTEMPTS_MESSAGE;
        }

        if (!Boolean.TRUE.equals(ops().setIfAbsent(userKey, writeUser(from), remaining))) {
            String winner = ops().get(userKey);
            return winner == null ? EXPIRED_MESSAGE : resumeClaimedRequest(requestId, readUser(winner), from);
        }
        sendConfirmationPrompt(requestId, from.id(), linkedAccount);
        return null;
    }

    @Override
    public void handleCallback(TelegramCallbackQuery callbackQuery) {
        String[] parts = callbackQuery.data() == null ? new String[0] : callbackQuery.data().split(":");
        TelegramMessage source = callbackQuery.message();
        TelegramUser from = callbackQuery.from();
        if (parts.length != 3 || !(CONFIRM.equals(parts[1]) || CANCEL.equals(parts[1])) || !isWellFormed(parts[2])
                || from == null || from.id() == null || source == null || source.chat() == null) {
            telegramApiClient.answerCallbackQuery(callbackQuery.id(), "This login request is no longer valid.");
            return;
        }

        String requestId = parts[2];
        boolean confirmed = CONFIRM.equals(parts[1]);
        Long chatId = source.chat().id();
        Duration remaining = remainingTtl(requestId);
        String claimant = remaining == null ? null : ops().get(RedisKeys.telegramLoginUserKey(requestId));
        if (claimant == null) {
            telegramApiClient.answerCallbackQuery(callbackQuery.id(), "This login request has expired.");
            telegramApiClient.editMessageText(chatId, source.messageId(), EXPIRED_MESSAGE);
            return;
        }
        if (!from.id().equals(readUser(claimant).id())) {
            telegramApiClient.answerCallbackQuery(callbackQuery.id(),
                    "This login request belongs to another Telegram account.");
            return;
        }

        TelegramLoginStatus decision = confirmed ? TelegramLoginStatus.APPROVED : TelegramLoginStatus.REJECTED;
        if (!Boolean.TRUE.equals(ops().setIfAbsent(RedisKeys.telegramLoginDecisionKey(requestId), decision.name(),
                remaining))) {
            telegramApiClient.answerCallbackQuery(callbackQuery.id(), "You've already answered this login request.");
            return;
        }

        telegramApiClient.answerCallbackQuery(callbackQuery.id(), confirmed ? "Login confirmed" : "Login cancelled");
        telegramApiClient.editMessageText(chatId, source.messageId(), confirmed
                ? "✅ <b>Login confirmed.</b> Go back to your browser — you'll be signed in automatically."
                : "✖️ <b>Login cancelled.</b> Nobody was signed in.");
    }

    private String resumeClaimedRequest(String requestId, TelegramUser claimant, TelegramUser from) {
        if (!claimant.id().equals(from.id())) {
            return CLAIMED_ELSEWHERE_MESSAGE;
        }
        if (ops().get(RedisKeys.telegramLoginDecisionKey(requestId)) != null) {
            return ALREADY_ANSWERED_MESSAGE;
        }
        sendConfirmationPrompt(requestId, from.id(), userRepository.findByTelegramChatId(from.id().toString()));
        return null;
    }

    private void sendConfirmationPrompt(String requestId, Long chatId, Optional<User> linkedAccount) {
        String account = linkedAccount
                .map(user -> "You'll be signed in as <b>"
                        + TelegramFormat.escape(TelegramFormat.titleCase(user.getFullName())) + "</b>.")
                .orElse("No 590st Cafe account is linked to this Telegram yet, so confirming creates one for you.");
        String html = "🔐 <b>Log in to 590st Cafe?</b>\n\n"
                + "A browser asked to log in to the 590st Cafe website with this Telegram account. " + account + "\n\n"
                + "Only tap <b>Confirm</b> if you just tapped <b>Log in with Telegram</b> yourself. "
                + "If it wasn't you, tap Cancel and nobody will be signed in.";
        telegramApiClient.sendHtmlMessageWithInlineKeyboard(chatId, html, List.of(List.of(
                Map.of("text", "✅ Confirm login", "callback_data", CALLBACK_PREFIX + CONFIRM + ":" + requestId),
                Map.of("text", "✖️ Cancel", "callback_data", CALLBACK_PREFIX + CANCEL + ":" + requestId))));
    }

    private boolean exceedsAttemptLimit(Long telegramUserId) {
        String key = RedisKeys.TELEGRAM_LOGIN_ATTEMPTS_PREFIX + telegramUserId;
        Long attempts = ops().increment(key);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(properties.getLoginAttemptWindowSeconds()));
        }
        return attempts != null && attempts > properties.getLoginMaxAttempts();
    }

    private Duration remainingTtl(String requestId) {
        Long millis = redisTemplate.getExpire(RedisKeys.telegramLoginKey(requestId), TimeUnit.MILLISECONDS);
        return millis == null || millis <= 0 ? null : Duration.ofMillis(millis);
    }

    private boolean isWellFormed(String requestId) {
        return requestId != null && REQUEST_ID.matcher(requestId).matches();
    }

    private ValueOperations<String, String> ops() {
        return redisTemplate.opsForValue();
    }

    private String writeUser(TelegramUser user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not store the Telegram login identity", ex);
        }
    }

    private TelegramUser readUser(String json) {
        try {
            return objectMapper.readValue(json, TelegramUser.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not read the Telegram login identity", ex);
        }
    }

    private static String randomToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
