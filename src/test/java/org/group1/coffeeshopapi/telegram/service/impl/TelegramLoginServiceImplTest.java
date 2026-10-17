package org.group1.coffeeshopapi.telegram.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramLoginServiceImplTest {

    private static final Long ALICE = 9_001L;
    private static final Long MALLORY = 6_666L;

    @Mock private UserRepository userRepository;
    @Mock private TelegramApiClient telegramApiClient;

    private final FakeRedis redis = new FakeRedis();
    private final TelegramProperties properties = new TelegramProperties();
    private TelegramLoginServiceImpl service;

    @BeforeEach
    void setUp() {
        properties.setBotToken("123456:test-token");
        properties.setBotUsername("@CafeBot");
        properties.setWebhookSecret("webhook-secret");
        service = new TelegramLoginServiceImpl(redis.template, userRepository, properties, telegramApiClient,
                new ObjectMapper());
    }

    @Test
    void refusesToStartWhenTheBotIsNotFullyConfigured() {
        properties.setWebhookSecret("");

        assertThatThrownBy(() -> service.createChallenge())
                .isInstanceOf(TelegramDeliveryException.class)
                .satisfies(ex -> assertThat(((TelegramDeliveryException) ex).getReason())
                        .isEqualTo(TelegramDeliveryException.Reason.NOT_CONFIGURED))
                .hasMessageContaining("isn't available");
        assertThat(redis.values).isEmpty();
    }

    @Test
    void aNewChallengeHasADeepLinkToTheBotAndStoresOnlyAHashOfThePollToken() {
        TelegramLoginChallenge first = service.createChallenge();
        TelegramLoginChallenge second = service.createChallenge();

        assertThat(first.requestId()).matches("[A-Za-z0-9_-]{22}").isNotEqualTo(second.requestId());
        assertThat(first.pollToken()).hasSizeGreaterThanOrEqualTo(43).isNotEqualTo(second.pollToken());
        assertThat(first.deepLink()).isEqualTo("https://t.me/CafeBot?start=login_" + first.requestId());
        assertThat(first.botUsername()).isEqualTo("CafeBot");
        assertThat(first.expiresInSeconds()).isEqualTo(300);
        assertThat(redis.values.values()).noneMatch(value -> value.contains(first.pollToken()));
    }

    @Test
    void theFullFlowApprovesOnceAndThenTheRequestCanNeverBeReused() {
        linkedAccount(ALICE, "alice smith");
        TelegramLoginChallenge challenge = service.createChallenge();
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.PENDING);

        assertThat(service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"))).isNull();
        List<List<Map<String, String>>> keyboard = promptKeyboardSentTo(ALICE);
        assertThat(keyboard.get(0)).extracting(button -> button.get("callback_data"))
                .containsExactly("tglogin:ok:" + challenge.requestId(), "tglogin:no:" + challenge.requestId());
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.AWAITING_CONFIRMATION);

        service.handleCallback(tap(ALICE, "tglogin:ok:" + challenge.requestId()));
        verify(telegramApiClient).answerCallbackQuery("cbq", "Login confirmed");
        verify(telegramApiClient).editMessageText(eq(ALICE), eq(77L), anyString());

        TelegramLoginOutcome outcome = service.checkChallenge(challenge.requestId(), challenge.pollToken());
        assertThat(outcome.status()).isEqualTo(TelegramLoginStatus.APPROVED);
        assertThat(outcome.telegramUser().id()).isEqualTo(ALICE);
        assertThat(outcome.telegramUser().username()).isEqualTo("alice");
        assertThat(outcome.telegramUser().lastName()).isEqualTo("Smith");

        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.EXPIRED);
        assertThat(redis.values).doesNotContainKeys(
                "tg:login:" + challenge.requestId(),
                "tg:login:" + challenge.requestId() + ":user",
                "tg:login:" + challenge.requestId() + ":decision");
    }

    @Test
    void aWrongPollTokenLooksExpiredAndDoesNotConsumeTheRealRequest() {
        TelegramLoginChallenge challenge = service.createChallenge();
        approve(challenge, ALICE, "alice");

        assertThat(service.checkChallenge(challenge.requestId(), "guessed-token").status())
                .isEqualTo(TelegramLoginStatus.EXPIRED);
        assertThat(service.checkChallenge(challenge.requestId(), challenge.pollToken()).status())
                .isEqualTo(TelegramLoginStatus.APPROVED);
    }

    @Test
    void anExpiredRequestIsRejectedInTheBrowserAndInTelegram() {
        TelegramLoginChallenge challenge = service.createChallenge();
        service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"));

        redis.advance(Duration.ofSeconds(301));

        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.EXPIRED);
        assertThat(service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"))).contains("expired");
        service.handleCallback(tap(ALICE, "tglogin:ok:" + challenge.requestId()));
        verify(telegramApiClient).answerCallbackQuery("cbq", "This login request has expired.");
    }

    @Test
    void cancellingInTelegramRejectsTheLoginOnce() {
        TelegramLoginChallenge challenge = service.createChallenge();
        service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"));

        service.handleCallback(tap(ALICE, "tglogin:no:" + challenge.requestId()));

        verify(telegramApiClient).answerCallbackQuery("cbq", "Login cancelled");
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.REJECTED);
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.EXPIRED);
    }

    @Test
    void theFirstAnswerWinsSoALaterTapCannotFlipIt() {
        TelegramLoginChallenge challenge = service.createChallenge();
        service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"));

        service.handleCallback(tap(ALICE, "tglogin:ok:" + challenge.requestId()));
        service.handleCallback(tap(ALICE, "tglogin:no:" + challenge.requestId()));

        verify(telegramApiClient).answerCallbackQuery("cbq", "You've already answered this login request.");
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.APPROVED);
    }

    @Test
    void aSecondTelegramAccountCannotHijackALinkSomeoneElseAlreadyOpened() {
        TelegramLoginChallenge challenge = service.createChallenge();
        service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"));

        String reply = service.handleStart(challenge.requestId(), privateMessage(MALLORY, "mallory"));
        service.handleCallback(tap(MALLORY, "tglogin:ok:" + challenge.requestId()));

        assertThat(reply).contains("different Telegram account");
        verify(telegramApiClient).answerCallbackQuery("cbq", "This login request belongs to another Telegram account.");
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.AWAITING_CONFIRMATION);
    }

    @Test
    void reopeningYourOwnLinkResendsThePromptWithoutCountingAnotherAttempt() {
        TelegramLoginChallenge challenge = service.createChallenge();

        service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"));
        service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"));

        verify(telegramApiClient, times(2)).sendHtmlMessageWithInlineKeyboard(eq(ALICE), anyString(), any());
        assertThat(redis.values.get("tg:login-attempts:" + ALICE)).isEqualTo("1");
    }

    @Test
    void loginLinksOnlyWorkInAPrivateChatWithTheBot() {
        TelegramLoginChallenge challenge = service.createChallenge();
        TelegramMessage groupMessage = new TelegramMessage(77L, new TelegramChat(-100L, "group"),
                new TelegramUser(ALICE, "Alice", "Smith", "alice"), "/start", null);

        assertThat(service.handleStart(challenge.requestId(), groupMessage)).contains("private chat");
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.PENDING);
    }

    @Test
    void anUnlinkedTelegramAccountWithoutAUsernameIsAskedToSetOneFirst() {
        TelegramLoginChallenge challenge = service.createChallenge();

        assertThat(service.handleStart(challenge.requestId(), privateMessage(ALICE, null))).contains("username");
        assertThat(check(challenge)).isEqualTo(TelegramLoginStatus.PENDING);
        verify(telegramApiClient, never()).sendHtmlMessageWithInlineKeyboard(anyLong(), anyString(), any());
    }

    @Test
    void anUnlinkedTelegramAccountIsToldThatConfirmingCreatesAnAccount() {
        TelegramLoginChallenge challenge = service.createChallenge();

        service.handleStart(challenge.requestId(), privateMessage(ALICE, "alice"));

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(telegramApiClient).sendHtmlMessageWithInlineKeyboard(eq(ALICE), html.capture(), any());
        assertThat(html.getValue()).contains("creates one for you");
    }

    @Test
    void aLinkedTelegramAccountIsToldWhichAccountItWillSignInto() {
        linkedAccount(ALICE, "alice smith");
        TelegramLoginChallenge challenge = service.createChallenge();

        service.handleStart(challenge.requestId(), privateMessage(ALICE, null));

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(telegramApiClient).sendHtmlMessageWithInlineKeyboard(eq(ALICE), html.capture(), any());
        assertThat(html.getValue()).contains("signed in as <b>Alice Smith</b>");
    }

    @Test
    void tooManyLoginAttemptsFromOneTelegramAccountAreRateLimited() {
        for (int i = 0; i < properties.getLoginMaxAttempts(); i++) {
            assertThat(service.handleStart(service.createChallenge().requestId(), privateMessage(ALICE, "alice")))
                    .isNull();
        }
        TelegramLoginChallenge oneTooMany = service.createChallenge();

        assertThat(service.handleStart(oneTooMany.requestId(), privateMessage(ALICE, "alice")))
                .contains("Too many login attempts");
        assertThat(check(oneTooMany)).isEqualTo(TelegramLoginStatus.PENDING);

        redis.advance(Duration.ofSeconds(properties.getLoginAttemptWindowSeconds() + 1));
        TelegramLoginChallenge afterTheWindow = service.createChallenge();
        assertThat(service.handleStart(afterTheWindow.requestId(), privateMessage(ALICE, "alice"))).isNull();
    }

    @Test
    void malformedRequestIdsAndCallbackDataAreRejectedWithoutTouchingRedisState() {
        assertThat(service.checkChallenge("../../etc", "x").status()).isEqualTo(TelegramLoginStatus.EXPIRED);
        assertThat(service.handleStart("not-a-real-id", privateMessage(ALICE, "alice"))).contains("expired");

        service.handleCallback(tap(ALICE, "tglogin:maybe:AAAAAAAAAAAAAAAAAAAAAA"));

        verify(telegramApiClient).answerCallbackQuery("cbq", "This login request is no longer valid.");
        assertThat(redis.values).isEmpty();
    }

    private TelegramLoginStatus check(TelegramLoginChallenge challenge) {
        return service.checkChallenge(challenge.requestId(), challenge.pollToken()).status();
    }

    private void approve(TelegramLoginChallenge challenge, Long telegramId, String username) {
        service.handleStart(challenge.requestId(), privateMessage(telegramId, username));
        service.handleCallback(tap(telegramId, "tglogin:ok:" + challenge.requestId()));
    }

    private void linkedAccount(Long telegramId, String fullName) {
        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setTelegramChatId(telegramId.toString());
        when(userRepository.findByTelegramChatId(telegramId.toString())).thenReturn(Optional.of(customer));
    }

    @SuppressWarnings("unchecked")
    private List<List<Map<String, String>>> promptKeyboardSentTo(Long chatId) {
        ArgumentCaptor<List<List<Map<String, String>>>> keyboard = ArgumentCaptor.forClass(List.class);
        verify(telegramApiClient).sendHtmlMessageWithInlineKeyboard(eq(chatId), anyString(), keyboard.capture());
        return keyboard.getValue();
    }

    private static TelegramMessage privateMessage(Long telegramId, String username) {
        return new TelegramMessage(77L, new TelegramChat(telegramId, "private"),
                new TelegramUser(telegramId, "Alice", "Smith", username), "/start", null);
    }

    private static TelegramCallbackQuery tap(Long telegramId, String data) {
        TelegramMessage prompt = new TelegramMessage(77L, new TelegramChat(telegramId, "private"), null, "prompt", null);
        return new TelegramCallbackQuery("cbq", new TelegramUser(telegramId, "Alice", "alice"), prompt, data);
    }

    @SuppressWarnings("unchecked")
    private static final class FakeRedis {
        final Map<String, String> values = new HashMap<>();
        final Map<String, Long> expiresAt = new HashMap<>();
        final StringRedisTemplate template = mock(StringRedisTemplate.class);
        final ValueOperations<String, String> ops = mock(ValueOperations.class);
        long now = 0;

        FakeRedis() {
            lenient().when(template.opsForValue()).thenReturn(ops);
            lenient().when(ops.get(anyString())).thenAnswer(call -> read(call.getArgument(0)));
            lenient().doAnswer(call -> {
                write(call.getArgument(0), call.getArgument(1), call.getArgument(2));
                return null;
            }).when(ops).set(anyString(), anyString(), any(Duration.class));
            lenient().when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(call -> {
                if (read(call.getArgument(0)) != null) {
                    return false;
                }
                write(call.getArgument(0), call.getArgument(1), call.getArgument(2));
                return true;
            });
            lenient().when(ops.increment(anyString())).thenAnswer(call -> {
                String key = call.getArgument(0);
                String current = read(key);
                long next = current == null ? 1 : Long.parseLong(current) + 1;
                values.put(key, Long.toString(next));
                if (current == null) {
                    expiresAt.remove(key);
                }
                return next;
            });
            lenient().when(template.expire(anyString(), any(Duration.class))).thenAnswer(call -> {
                String key = call.getArgument(0);
                if (read(key) == null) {
                    return false;
                }
                expiresAt.put(key, now + ((Duration) call.getArgument(1)).toMillis());
                return true;
            });
            lenient().when(template.getExpire(anyString(), eq(TimeUnit.MILLISECONDS))).thenAnswer(call -> {
                String key = call.getArgument(0);
                if (read(key) == null) {
                    return -2L;
                }
                Long deadline = expiresAt.get(key);
                return deadline == null ? -1L : deadline - now;
            });
            lenient().when(template.delete(anyString())).thenAnswer(call -> remove(call.getArgument(0)));
            lenient().when(template.delete(anyCollection())).thenAnswer(call -> ((Collection<String>) call.getArgument(0))
                    .stream().filter(this::remove).count());
        }

        void advance(Duration duration) {
            now += duration.toMillis();
        }

        private String read(String key) {
            Long deadline = expiresAt.get(key);
            if (deadline != null && deadline <= now) {
                values.remove(key);
                expiresAt.remove(key);
            }
            return values.get(key);
        }

        private void write(String key, String value, Duration ttl) {
            values.put(key, value);
            expiresAt.put(key, now + ttl.toMillis());
        }

        private boolean remove(String key) {
            boolean existed = read(key) != null;
            values.remove(key);
            expiresAt.remove(key);
            return existed;
        }
    }
}
