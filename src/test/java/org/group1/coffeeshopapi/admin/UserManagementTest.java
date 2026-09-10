package org.group1.coffeeshopapi.admin;

import org.group1.coffeeshopapi.admin.controller.UserAdminController;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.admin.service.impl.UserAdminServiceImpl;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.mapper.UserMapper;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.group1.coffeeshopapi.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserManagementTest {
    @Mock UserRepository userRepository;
    @Mock AdminRepository adminRepository;
    @Mock BaristaRepository baristaRepository;
    @Mock CustomerRepository customerRepository;
    @Mock UserMapper userMapper;
    @Mock TokenService tokenService;
    @Mock AuthUserSyncService authUserSyncService;
    @Mock UserProfileService userProfileService;
    @InjectMocks UserAdminServiceImpl service;
    MockMvc mvc;

    @BeforeEach
    void setupController() {
        mvc = MockMvcBuilders.standaloneSetup(new UserAdminController(service)).build();
    }

    @Test
    void profileEndpointUsesSharedProfileValidationAndUpdates() throws Exception {
        UUID id = UUID.randomUUID();
        var body = new UpdateProfileRequest("Updated Customer", "", null);
        when(userProfileService.updateProfile(id, body)).thenReturn(UserResponse.builder()
                .id(id).fullName(body.fullName()).role(Role.CUSTOMER).status(UserStatus.ACTIVE).build());
        mvc.perform(patch("/api/admin/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Customer\",\"phoneNumber\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Customer"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
        verify(userProfileService).updateProfile(id, body);
    }

    @Test
    void profileEndpointRejectsInvalidFields() throws Exception {
        mvc.perform(patch("/api/admin/users/{id}", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\",\"phoneNumber\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userProfileService);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"ADMIN", "BARISTA", "CUSTOMER"})
    void deleteEndpointRevokesAccessAndRetainsStoredRecords(Role role) throws Exception {
        User account = switch (role) {
            case ADMIN -> new Admin();
            case BARISTA -> new Barista();
            case CUSTOMER -> new Customer();
            default -> throw new AssertionError(role);
        };
        account.setId(UUID.randomUUID());
        account.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(account.getId())).thenReturn(Optional.of(account));

        mvc.perform(delete("/api/admin/users/{id}", account.getId())).andExpect(status().isOk());

        assertEquals(UserStatus.DELETED, account.getStatus());
        assertFalse(new CustomUserDetails(account).isEnabled());
        verify(userRepository).save(account);
        verify(authUserSyncService).sync(account);
        verify(tokenService).revokeRefreshToken(account.getId());
        verify(userRepository, never()).delete(any());
        verify(authUserSyncService, never()).remove(any());
    }

    @Test
    void deletingMissingAccountDoesNotRevokeAnotherSession() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));
        verifyNoInteractions(tokenService, authUserSyncService);
    }
}
