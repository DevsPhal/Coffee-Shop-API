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

// Keeps auth_users in sync with every account. Call sync() after saving a User, and remove()
// after deleting one.
@Service
@RequiredArgsConstructor
public class AuthUserSyncService {

    private final AuthUserRepository authUserRepository;

    public void sync(User user) {
        sync(user.getId(), user.getRole(), user.getFullName(), user.getStatus());
    }

    // The Super Admin has no User row — call this after it logs in instead, so it still shows up.
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
