package org.group1.coffeeshopapi.admin;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.admin.service.impl.StaffServiceImpl;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.service.UserProfileService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffAvatarServiceTest {
    @Mock AdminRepository adminRepository;
    @Mock BaristaRepository baristaRepository;
    @Mock UserProfileService userProfileService;
    @InjectMocks StaffServiceImpl service;
    private final MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1});

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "BARISTA"})
    void uploadValidatesTargetRoleBeforeUsingSharedAvatarStorage(Role role) {
        UUID id = UUID.randomUUID();
        if (role == Role.ADMIN) {
            Admin account = new Admin();
            account.setId(id);
            when(adminRepository.findById(id)).thenReturn(Optional.of(account));
        } else {
            Barista account = new Barista();
            account.setId(id);
            when(baristaRepository.findById(id)).thenReturn(Optional.of(account));
        }
        UserResponse uploaded = UserResponse.builder().id(id).role(role).avatarUrl("/avatars/photo.png").build();
        when(userProfileService.uploadAvatar(id, file)).thenReturn(uploaded);
        assertSame(uploaded, service.uploadAvatar(id, file, role));
        verify(userProfileService).uploadAvatar(id, file);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "BARISTA"})
    void accountOutsideTargetRoleCannotReachAvatarStorage(Role role) {
        UUID otherRoleId = UUID.randomUUID();
        if (role == Role.ADMIN) when(adminRepository.findById(otherRoleId)).thenReturn(Optional.empty());
        else when(baristaRepository.findById(otherRoleId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.uploadAvatar(otherRoleId, file, role));
        verifyNoInteractions(userProfileService);
    }
}
