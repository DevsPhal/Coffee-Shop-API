package org.group1.coffeeshopapi.user.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.user.entity.AuthUser;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.AuthUserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Mirrors every create/update/delete on {@code Admin}/{@code Barista}/{@code Customer} into
 * {@code auth_users}, so that table lists every account across all roles in one place even
 * though each role now owns its own standalone table. Call {@link #sync} right after saving a
 * {@link User} and {@link #remove} right after deleting one.
 */
@Service
@RequiredArgsConstructor
public class AuthUserSyncService {

    private final AuthUserRepository authUserRepository;

    public void sync(User user) {
        AuthUser mirror = authUserRepository.findById(user.getId()).orElseGet(AuthUser::new);
        mirror.setId(user.getId());
        mirror.setRole(user.getRole());
        mirror.setFullName(user.getFullName());
        mirror.setEmail(user.getEmail());
        mirror.setPassword(user.getPassword());
        mirror.setPhoneNumber(user.getPhoneNumber());
        mirror.setGender(user.getGender());
        mirror.setStatus(user.getStatus());
        mirror.setTelegramChatId(user.getTelegramChatId());
        mirror.setCreatedAt(user.getCreatedAt());
        mirror.setUpdatedAt(user.getUpdatedAt());
        authUserRepository.save(mirror);
    }

    public void remove(UUID userId) {
        authUserRepository.deleteById(userId);
    }
}
