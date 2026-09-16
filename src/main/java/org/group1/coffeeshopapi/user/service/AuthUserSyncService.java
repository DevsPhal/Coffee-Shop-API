package org.group1.coffeeshopapi.user.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.security.SuperAdminUserDetails;
import org.group1.coffeeshopapi.user.entity.AuthUser;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.AuthUserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Keeps {@code auth_users} populated with one row per {@code Admin}/{@code Barista}/
 * {@code Customer}, plus the Super Admin — see {@link AuthUser}'s javadoc. Call {@link #sync}
 * right after saving a {@link User} (id/role never change, but name/status can — this always
 * writes the current values, so it's safe and cheap to call after every save) and
 * {@link #remove} right after deleting one.
 */
@Service
@RequiredArgsConstructor
public class AuthUserSyncService {

    private final AuthUserRepository authUserRepository;

    public void sync(User user) {
        sync(user.getId(), user.getRole(), user.getFullName(), user.getStatus());
    }

    // The Super Admin has no User row to sync from (see SuperAdminUserDetails) — call this
    // instead right after it authenticates (AuthServiceImpl#login) so it still shows up in
    // auth_users like every other account.
    public void syncSuperAdmin() {
        sync(SuperAdminUserDetails.ID, Role.SUPER_ADMIN, SuperAdminUserDetails.DISPLAY_NAME, UserStatus.ACTIVE);
    }

    public void remove(UUID userId) {
        authUserRepository.deleteById(userId);
    }

    private void sync(UUID id, Role role, String name, UserStatus status) {
        AuthUser pointer = authUserRepository.findById(id).orElseGet(AuthUser::new);
        pointer.setId(id);
        pointer.setRole(role);
        pointer.setName(name);
        pointer.setStatus(status);
        authUserRepository.save(pointer);
    }
}
