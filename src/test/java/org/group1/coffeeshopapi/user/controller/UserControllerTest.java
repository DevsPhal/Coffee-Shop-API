package org.group1.coffeeshopapi.user.controller;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.SuperAdminResponse;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The super admin has no {@code User} row and its profile is fixed by configuration (see
 * {@link SuperAdminUserDetails}) — every self-service write endpoint here has to turn it away
 * instead of touching {@link UserProfileService}, while a real account's requests pass straight
 * through. Exercised as a plain unit test (no MockMvc/Spring context) since the branching is on
 * the resolved {@code UserDetails} type, not on HTTP concerns.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserMapper userMapper;
    @Mock private TelegramLinkService telegramLinkService;
    @Mock private UserProfileService userProfileService;
    @InjectMocks private UserController controller;

    private final SuperAdminUserDetails superAdmin = new SuperAdminUserDetails("admin@590stcafe.local", "hash");

    @Test
    void meReturnsTheFixedProfileForTheConfigDrivenSuperAdmin() {
        ApiResponse<Object> response = controller.me(superAdmin);

        assertThat(response.getData()).isInstanceOf(SuperAdminResponse.class);
        SuperAdminResponse profile = (SuperAdminResponse) response.getData();
        assertThat(profile.fullName()).isEqualTo(SuperAdminUserDetails.DISPLAY_NAME);
        assertThat(profile.email()).isEqualTo("admin@590stcafe.local");
        assertThat(profile.role()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(profile.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void meReturnsTheMappedProfileForARealAccount() {
        Admin admin = adminAccount();
        CustomUserDetails principal = new CustomUserDetails(admin);
        UserResponse expected = UserResponse.builder().id(admin.getId()).role(Role.ADMIN).build();
        when(userMapper.toResponse(admin)).thenReturn(expected);

        ApiResponse<Object> response = controller.me(principal);

        assertThat(response.getData()).isSameAs(expected);
    }

    @Test
    void updateMeRejectsTheSuperAdminWithAClearReason() {
        var request = new UpdateProfileRequest("New Name", null, null);

        assertThatThrownBy(() -> controller.updateMe(request, superAdmin))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("fixed and cannot be edited");
        verifyNoInteractions(userProfileService);
    }

    @Test
    void updateMeDelegatesToUserProfileServiceForARealAccount() {
        Admin admin = adminAccount();
        CustomUserDetails principal = new CustomUserDetails(admin);
        var request = new UpdateProfileRequest("New Name", null, null);
        UserResponse expected = UserResponse.builder().id(admin.getId()).fullName("New Name").role(Role.ADMIN).build();
        when(userProfileService.updateProfile(admin.getId(), request)).thenReturn(expected);

        ApiResponse<UserResponse> response = controller.updateMe(request, principal);

        assertThat(response.getData()).isSameAs(expected);
    }

    @Test
    void uploadAvatarRejectsTheSuperAdmin() {
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> controller.uploadAvatar(file, superAdmin))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("does not have an avatar");
        verifyNoInteractions(userProfileService);
    }

    @Test
    void removeAvatarRejectsTheSuperAdmin() {
        assertThatThrownBy(() -> controller.removeAvatar(superAdmin))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("does not have an avatar");
        verifyNoInteractions(userProfileService);
    }

    private Admin adminAccount() {
        Admin admin = new Admin();
        admin.setId(UUID.randomUUID());
        admin.setStatus(UserStatus.ACTIVE);
        return admin;
    }
}
