package org.group1.coffeeshopapi.user.service.impl;

import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidCredentialsException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.user.dto.request.ChangePasswordRequest;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers profile self-service — the blank-phone-clears convention, duplicate-phone handling, and
 * the password-change safety checks (wrong current password, new == old, refresh token
 * revocation) — none of which had test coverage before. See UserProfileServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private FileStorageService fileStorageService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private AuthUserSyncService authUserSyncService;
    @InjectMocks private UserProfileServiceImpl service;

    @Test
    void updatingWithABlankPhoneNumberClearsItRatherThanRejectingIt() {
        User user = customer();
        user.setPhoneNumber("012 345 678");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenReturn(user);

        service.updateProfile(user.getId(), new UpdateProfileRequest(null, "   ", null));

        assertThat(user.getPhoneNumber()).isNull();
    }

    @Test
    void updatingFullNameToOnlyWhitespaceIsRejected() {
        User user = customer();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.updateProfile(user.getId(), new UpdateProfileRequest("   ", null, null)))
                .isInstanceOf(InvalidOperationException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatingToAPhoneNumberAlreadyUsedByAnotherAccountIsReportedAsADuplicate() {
        User user = customer();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(user)).thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> service.updateProfile(user.getId(), new UpdateProfileRequest(null, "099 888 777", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void changingPasswordWithTheWrongCurrentPasswordIsRejected() {
        User user = customer();
        user.setPassword("hashed-current");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-current", "hashed-current")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(user.getId(),
                new ChangePasswordRequest("wrong-current", "Newpass!23")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(tokenService, never()).revokeRefreshToken(any());
    }

    @Test
    void changingPasswordToTheSamePasswordItAlreadyHasIsRejected() {
        User user = customer();
        user.setPassword("hashed-current");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Correct!23", "hashed-current")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(user.getId(),
                new ChangePasswordRequest("Correct!23", "Correct!23")))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void changingPasswordSuccessfullyRevokesEveryOtherSessionsRefreshToken() {
        User user = customer();
        user.setPassword("hashed-current");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Correct!23", "hashed-current")).thenReturn(true);
        when(passwordEncoder.matches("Newpass!23", "hashed-current")).thenReturn(false);
        when(passwordEncoder.encode("Newpass!23")).thenReturn("hashed-new");

        service.changePassword(user.getId(), new ChangePasswordRequest("Correct!23", "Newpass!23"));

        assertThat(user.getPassword()).isEqualTo("hashed-new");
        verify(tokenService).revokeRefreshToken(user.getId());
    }

    @Test
    void uploadingANewAvatarDeletesThePreviousOne() {
        User user = customer();
        user.setAvatarUrl("old-avatar-url");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(fileStorageService.uploadImage(any(MultipartFile.class), any())).thenReturn("new-avatar-url");
        when(userRepository.save(user)).thenReturn(user);

        service.uploadAvatar(user.getId(), org.mockito.Mockito.mock(MultipartFile.class));

        assertThat(user.getAvatarUrl()).isEqualTo("new-avatar-url");
        verify(fileStorageService).delete("old-avatar-url");
    }

    @Test
    void removingAnAvatarThatIsAlreadyClearIsANoOp() {
        User user = customer();
        user.setAvatarUrl(null);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        service.removeAvatar(user.getId());

        verify(fileStorageService, never()).delete(any());
        verify(userRepository, never()).save(any());
    }

    private User customer() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFullName("Sophal Nem");
        return customer;
    }
}
