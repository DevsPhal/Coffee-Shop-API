package org.group1.coffeeshopapi.user.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.user.entity.AuthUser;
import org.group1.coffeeshopapi.user.entity.User;
import org.group1.coffeeshopapi.user.repository.AuthUserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Keeps {@code auth_users} populated with one role-indexed pointer row per {@code Admin}/
 * {@code Barista}/{@code Customer} — see {@link AuthUser}'s javadoc for why it's just a pointer
 * and not a copy of their columns. Call {@link #sync} right after saving a {@link User} and
 * {@link #remove} right after deleting one.
 */
@Service
@RequiredArgsConstructor
public class AuthUserSyncService {

    private final AuthUserRepository authUserRepository;

    // id and role never change for the lifetime of an account, so once this pointer row exists
    // there's nothing left to update — safe (and a no-op) to call this again after every save,
    // not just the first one, without re-checking or re-writing anything.
    public void sync(User user) {
        if (authUserRepository.existsById(user.getId())) {
            return;
        }
        AuthUser pointer = new AuthUser();
        pointer.setId(user.getId());
        pointer.setRole(user.getRole());
        authUserRepository.save(pointer);
    }

    public void remove(UUID userId) {
        authUserRepository.deleteById(userId);
    }
}
