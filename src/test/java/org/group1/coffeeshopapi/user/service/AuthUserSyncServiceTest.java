package org.group1.coffeeshopapi.user.service;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.user.entity.AuthUser;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserSyncServiceTest {

    @Mock private AuthUserRepository authUserRepository;
    @InjectMocks private AuthUserSyncService service;

    @Test
    void syncCreatesANewRowWithTheAccountsCurrentNameRoleAndStatus() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFullName("Test Customer");
        customer.setStatus(UserStatus.ACTIVE);
        when(authUserRepository.findById(customer.getId())).thenReturn(Optional.empty());
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sync(customer);

        ArgumentCaptor<AuthUser> saved = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(customer.getId());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getValue().getName()).isEqualTo("Test Customer");
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void syncOverwritesTheNameAndStatusOfAnAlreadyIndexedAccount() {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setFullName("Renamed Customer");
        customer.setStatus(UserStatus.SUSPENDED);
        AuthUser existing = new AuthUser();
        existing.setId(customer.getId());
        existing.setRole(Role.CUSTOMER);
        existing.setName("Old Name");
        existing.setStatus(UserStatus.ACTIVE);
        when(authUserRepository.findById(customer.getId())).thenReturn(Optional.of(existing));
        when(authUserRepository.save(existing)).thenReturn(existing);

        service.sync(customer);

        assertThat(existing.getName()).isEqualTo("Renamed Customer");
        assertThat(existing.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(authUserRepository).save(existing);
    }

    @Test
    void syncSuperAdminUpsertsAFixedRowForTheConfigDrivenAccount() {
        when(authUserRepository.findById(SuperAdminUserDetails.ID)).thenReturn(Optional.empty());
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncSuperAdmin();

        ArgumentCaptor<AuthUser> saved = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(SuperAdminUserDetails.ID);
        assertThat(saved.getValue().getRole()).isEqualTo(Role.SUPER_ADMIN);
        assertThat(saved.getValue().getName()).isEqualTo(SuperAdminUserDetails.DISPLAY_NAME);
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void removeDeletesTheIndexRowById() {
        UUID id = UUID.randomUUID();

        service.remove(id);

        verify(authUserRepository).deleteById(id);
    }
}
