package org.group1.coffeeshopapi.user.controller;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.user.dto.request.ChangePasswordRequest;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.SuperAdminResponse;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.service.SuperAdminProfileService;
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

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserMapper userMapper;
    @Mock private TelegramLinkService telegramLinkService;
    @Mock private UserProfileService userProfileService;
    @Mock private SuperAdminProfileService superAdminProfileService;
    @InjectMocks private UserController controller;

    private final SuperAdminUserDetails superAdmin = new SuperAdminUserDetails("admin@590stcafe.local", "hash");

    @Test
    void meReturnsTheSavedProfileForTheConfigDrivenSuperAdmin() {
        SuperAdminResponse expected = superAdminProfile("Sokha Chan", "https://cdn/avatars/me.png");
        when(superAdminProfileService.getProfile("admin@590stcafe.local")).thenReturn(expected);

        ApiResponse<Object> response = controller.me(superAdmin);

        assertThat(response.getData()).isSameAs(expected);
        verifyNoInteractions(userMapper);
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
    void updateMeSavesTheSuperAdminsOwnProfile() {
        var request = new UpdateProfileRequest("Sokha Chan", null, null);
        SuperAdminResponse expected = superAdminProfile("Sokha Chan", null);
        when(superAdminProfileService.updateProfile("admin@590stcafe.local", request)).thenReturn(expected);

        ApiResponse<Object> response = controller.updateMe(request, superAdmin);

        assertThat(response.getData()).isSameAs(expected);
        verifyNoInteractions(userProfileService);
    }

    @Test
    void updateMeDelegatesToUserProfileServiceForARealAccount() {
        Admin admin = adminAccount();
        CustomUserDetails principal = new CustomUserDetails(admin);
        var request = new UpdateProfileRequest("New Name", null, null);
        UserResponse expected = UserResponse.builder().id(admin.getId()).fullName("New Name").role(Role.ADMIN).build();
        when(userProfileService.updateProfile(admin.getId(), request)).thenReturn(expected);

        ApiResponse<Object> response = controller.updateMe(request, principal);

        assertThat(response.getData()).isSameAs(expected);
    }

    @Test
    void uploadAvatarStoresTheSuperAdminsPhoto() {
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});
        SuperAdminResponse expected = superAdminProfile("Super Admin", "https://cdn/avatars/new.png");
        when(superAdminProfileService.uploadAvatar("admin@590stcafe.local", file)).thenReturn(expected);

        ApiResponse<Object> response = controller.uploadAvatar(file, superAdmin);

        assertThat(response.getData()).isSameAs(expected);
        verifyNoInteractions(userProfileService);
    }

    @Test
    void removeAvatarClearsTheSuperAdminsPhoto() {
        SuperAdminResponse expected = superAdminProfile("Super Admin", null);
        when(superAdminProfileService.removeAvatar("admin@590stcafe.local")).thenReturn(expected);

        ApiResponse<Object> response = controller.removeAvatar(superAdmin);

        assertThat(response.getData()).isSameAs(expected);
        verifyNoInteractions(userProfileService);
    }

    @Test
    void changePasswordStillPointsTheSuperAdminAtTheDeploymentConfig() {
        var request = new ChangePasswordRequest("Old#Pass1", "New#Pass1");

        assertThatThrownBy(() -> controller.changePassword(request, superAdmin))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("SUPER_ADMIN_PASSWORD");
        verifyNoInteractions(userProfileService, superAdminProfileService);
    }

    private SuperAdminResponse superAdminProfile(String fullName, String avatarUrl) {
        return SuperAdminResponse.builder()
                .id(SuperAdminUserDetails.ID)
                .fullName(fullName)
                .email("admin@590stcafe.local")
                .avatarUrl(avatarUrl)
                .role(Role.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Admin adminAccount() {
        Admin admin = new Admin();
        admin.setId(UUID.randomUUID());
        admin.setStatus(UserStatus.ACTIVE);
        return admin;
    }
}
