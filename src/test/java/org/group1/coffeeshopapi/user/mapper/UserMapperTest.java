package org.group1.coffeeshopapi.user.mapper;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMapperTest {

    @Mock private ActorLookupService actorLookupService;
    @InjectMocks private UserMapper userMapper;

    // /api/users/me maps the user the JWT filter loaded, outside any session, so the barista's
    // lazy creator must never be read beyond its id — that used to fail /me with a 500.
    @Test
    void mapsAnAdminCreatedBaristaWithoutReadingTheLazyCreator() {
        UUID adminId = UUID.randomUUID();
        Admin detachedCreator = mock(Admin.class);
        when(detachedCreator.getId()).thenReturn(adminId);
        // Any other access on the mock returns null/defaults; a real detached proxy would throw.

        Barista barista = new Barista();
        barista.setId(UUID.randomUUID());
        barista.setFullName("sok dara");
        barista.setStatus(UserStatus.ACTIVE);
        barista.setCreatedByAdmin(detachedCreator);
        when(actorLookupService.resolve(adminId)).thenReturn(new ActorSummary(adminId, "Chan Admin", Role.ADMIN));

        UserResponse response = userMapper.toResponse(barista);

        assertThat(response.createdBy()).isEqualTo(adminId);
        assertThat(response.createdByName()).isEqualTo("Chan Admin");
        assertThat(response.createdByRole()).isEqualTo(Role.ADMIN);
    }
}
