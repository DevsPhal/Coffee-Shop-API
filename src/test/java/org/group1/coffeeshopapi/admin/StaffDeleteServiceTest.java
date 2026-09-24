package org.group1.coffeeshopapi.admin;

import org.group1.coffeeshopapi.admin.repository.AdminRepository;
import org.group1.coffeeshopapi.admin.service.impl.StaffServiceImpl;
import org.group1.coffeeshopapi.auth.service.TokenService;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.user.repository.UserRepository;
import org.group1.coffeeshopapi.user.service.AuthUserSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Deleting staff works like deleting any user: the row stays so attendance and order history
// keep their references, but the account can no longer log in.
@ExtendWith(MockitoExtension.class)
class StaffDeleteServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminRepository adminRepository;
    @Mock private BaristaRepository baristaRepository;
    @Mock private TokenService tokenService;
    @Mock private AuthUserSyncService authUserSyncService;
    @InjectMocks private StaffServiceImpl service;

    @Test
    void deletingABaristaSoftDeletesAndRevokesTheirSession() {
        Barista barista = new Barista();
        barista.setId(UUID.randomUUID());
        barista.setStatus(UserStatus.ACTIVE);
        when(baristaRepository.findById(barista.getId())).thenReturn(Optional.of(barista));

        service.delete(barista.getId(), Role.BARISTA);

        assertThat(barista.getStatus()).isEqualTo(UserStatus.DELETED);
        verify(userRepository).save(barista);
        verify(userRepository, never()).delete(any());
        verify(authUserSyncService).sync(barista);
        verify(tokenService).revokeRefreshToken(barista.getId());
    }
}
