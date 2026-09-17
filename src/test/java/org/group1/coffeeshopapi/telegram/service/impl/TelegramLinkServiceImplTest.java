package org.group1.coffeeshopapi.telegram.service.impl;

import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.TelegramContact;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the Telegram account-linking security checks — contact-card spoofing rejection and
 * phone-number matching for a staff invite — neither of which had test coverage before. See
 * TelegramLinkServiceImpl#resolveLinkCode/verifyPendingContact.
 */
@ExtendWith(MockitoExtension.class)
class TelegramLinkServiceImplTest {

    private static final Long CHAT_ID = 555L;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private UserRepository userRepository;
    @Mock private TelegramProperties properties;
    @Mock private AuthUserSyncService authUserSyncService;
    @Mock private TelegramApiClient telegramApiClient;
    @InjectMocks private TelegramLinkServiceImpl service;

    @Test
    void verifyPendingContactWithNoPendingInviteAsksForTheStartCommandInstead() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + CHAT_ID)).thenReturn(null);

        String result = service.verifyPendingContact(CHAT_ID, contact("85512345678", 999L), 999L);

        assertThat(result).contains("wasn't expecting");
        verify(userRepository, never()).findById(any());
    }

    @Test
    void verifyPendingContactRejectsAContactCardForwardedFromSomeoneElse() {
        UUID userId = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + CHAT_ID)).thenReturn(userId.toString());

        // The card's own userId (111) doesn't match whoever actually sent it (999) — a forwarded
        // contact, not the tapper's own.
        String result = service.verifyPendingContact(CHAT_ID, contact("85512345678", 111L), 999L);

        assertThat(result).contains("your own phone number");
        // The pending key is left in place so they can immediately retry with their own card.
        verify(redisTemplate, never()).delete(RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + CHAT_ID);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void verifyPendingContactRejectsAPhoneNumberThatDoesNotMatchTheInvite() {
        UUID userId = UUID.randomUUID();
        User invitee = pendingTelegramInvite(userId, "072 345 5674");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + CHAT_ID)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(invitee));

        String result = service.verifyPendingContact(CHAT_ID, contact("85599988877", 999L), 999L);

        assertThat(result).contains("doesn't match");
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyPendingContactActivatesTheAccountWhenThePhoneNumberMatches() {
        UUID userId = UUID.randomUUID();
        User invitee = pendingTelegramInvite(userId, "072 345 5674");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + CHAT_ID)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(invitee));
        when(userRepository.findByTelegramChatId(CHAT_ID.toString())).thenReturn(Optional.empty());

        String result = service.verifyPendingContact(CHAT_ID, contact("072 345 5674", 999L), 999L);

        assertThat(result).contains("Verified");
        assertThat(invitee.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(invitee.getTelegramChatId()).isEqualTo(CHAT_ID.toString());
        verify(redisTemplate).delete(RedisKeys.TELEGRAM_PENDING_CONTACT_PREFIX + CHAT_ID);
    }

    @Test
    void resolvingAnExpiredOrUnknownLinkCodeThrows() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.TELEGRAM_LINK_CODE_PREFIX + "ABC123")).thenReturn(null);

        assertThatThrownBy(() -> service.resolveLinkCode("abc123", CHAT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolvingALinkCodeForAnAlreadyLinkedAccountIsIdempotent() {
        UUID userId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(userId);
        customer.setFullName("Sophal");
        customer.setStatus(UserStatus.ACTIVE);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisKeys.TELEGRAM_LINK_CODE_PREFIX + "CODE12")).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(customer));
        when(userRepository.findByTelegramChatId(CHAT_ID.toString())).thenReturn(Optional.of(customer));

        String result = service.resolveLinkCode("code12", CHAT_ID);

        assertThat(result).contains("already linked");
        verify(userRepository, never()).save(any());
    }

    private TelegramContact contact(String phoneNumber, Long userId) {
        return new TelegramContact(phoneNumber, userId, "First", "Last");
    }

    private User pendingTelegramInvite(UUID userId, String phoneNumber) {
        Customer customer = new Customer();
        customer.setId(userId);
        customer.setFullName("New Hire");
        customer.setPhoneNumber(phoneNumber);
        customer.setStatus(UserStatus.PENDING_VERIFICATION);
        customer.setRegisterType(RegisterType.TELEGRAM);
        return customer;
    }
}
