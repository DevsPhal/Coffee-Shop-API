package org.group1.coffeeshopapi.user.service;

import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.SuperAdminResponse;
import org.group1.coffeeshopapi.user.entity.SuperAdminProfile;
import org.group1.coffeeshopapi.user.repository.SuperAdminProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminProfileServiceTest {

    private static final String EMAIL = "admin@590stcafe.local";

    @Mock private SuperAdminProfileRepository superAdminProfileRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private AuthUserSyncService authUserSyncService;
    @InjectMocks private SuperAdminProfileService service;

    @Test
    void getProfileShowsTheBuiltInDefaultsUntilAnythingIsSaved() {
        when(superAdminProfileRepository.findProfile()).thenReturn(Optional.empty());

        SuperAdminResponse profile = service.getProfile(EMAIL);

        assertThat(profile.id()).isEqualTo(SuperAdminUserDetails.ID);
        assertThat(profile.fullName()).isEqualTo(SuperAdminUserDetails.DISPLAY_NAME);
        assertThat(profile.email()).isEqualTo(EMAIL);
        assertThat(profile.avatarUrl()).isNull();
        assertThat(profile.role()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(profile.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateProfileCreatesTheRowAndSyncsTheNewName() {
        when(superAdminProfileRepository.findProfile()).thenReturn(Optional.empty());
        when(superAdminProfileRepository.save(any(SuperAdminProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuperAdminResponse profile = service.updateProfile(EMAIL,
                new UpdateProfileRequest("  Sokha Chan  ", "012 345 678", Gender.FEMALE));

        ArgumentCaptor<SuperAdminProfile> saved = ArgumentCaptor.forClass(SuperAdminProfile.class);
        verify(superAdminProfileRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(SuperAdminUserDetails.ID);
        assertThat(saved.getValue().getFullName()).isEqualTo("Sokha Chan");
        assertThat(saved.getValue().getPhoneNumber()).isEqualTo("012 345 678");
        assertThat(saved.getValue().getGender()).isEqualTo(Gender.FEMALE);
        verify(authUserSyncService).syncSuperAdmin();
        assertThat(profile.fullName()).isEqualTo("Sokha Chan");
        assertThat(profile.email()).isEqualTo(EMAIL);
    }

    @Test
    void updateProfileClearsThePhoneWhenItIsBlank() {
        when(superAdminProfileRepository.findProfile()).thenReturn(Optional.of(profile("Sokha Chan", null, "012 345 678")));
        when(superAdminProfileRepository.save(any(SuperAdminProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuperAdminResponse profile = service.updateProfile(EMAIL, new UpdateProfileRequest(null, " ", null));

        assertThat(profile.phoneNumber()).isNull();
        assertThat(profile.fullName()).isEqualTo("Sokha Chan");
    }

    @Test
    void updateProfileRejectsABlankName() {
        when(superAdminProfileRepository.findProfile()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(EMAIL, new UpdateProfileRequest("   ", null, null)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Full name cannot be blank");
        verify(superAdminProfileRepository, never()).save(any());
        verifyNoInteractions(authUserSyncService);
    }

    @Test
    void uploadAvatarStoresTheNewPhotoThenDeletesTheOldOne() {
        var file = new MockMultipartFile("file", "me.png", "image/png", new byte[]{1});
        when(superAdminProfileRepository.findProfile()).thenReturn(Optional.of(profile("Sokha Chan", "https://cdn/avatars/old.png", null)));
        when(fileStorageService.uploadImage(file, "avatars")).thenReturn("https://cdn/avatars/new.png");
        when(superAdminProfileRepository.save(any(SuperAdminProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuperAdminResponse profile = service.uploadAvatar(EMAIL, file);

        assertThat(profile.avatarUrl()).isEqualTo("https://cdn/avatars/new.png");
        verify(fileStorageService).delete("https://cdn/avatars/old.png");
    }

    @Test
    void removeAvatarDeletesTheFileAndClearsTheLink() {
        when(superAdminProfileRepository.findProfile()).thenReturn(Optional.of(profile("Sokha Chan", "https://cdn/avatars/me.png", null)));
        when(superAdminProfileRepository.save(any(SuperAdminProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SuperAdminResponse profile = service.removeAvatar(EMAIL);

        assertThat(profile.avatarUrl()).isNull();
        verify(fileStorageService).delete("https://cdn/avatars/me.png");
    }

    @Test
    void removeAvatarDoesNothingWhenThereIsNoPhoto() {
        when(superAdminProfileRepository.findProfile()).thenReturn(Optional.empty());

        SuperAdminResponse profile = service.removeAvatar(EMAIL);

        assertThat(profile.avatarUrl()).isNull();
        verifyNoInteractions(fileStorageService);
        verify(superAdminProfileRepository, never()).save(any());
    }

    private SuperAdminProfile profile(String fullName, String avatarUrl, String phoneNumber) {
        SuperAdminProfile profile = new SuperAdminProfile();
        profile.setId(SuperAdminUserDetails.ID);
        profile.setFullName(fullName);
        profile.setAvatarUrl(avatarUrl);
        profile.setPhoneNumber(phoneNumber);
        return profile;
    }
}
