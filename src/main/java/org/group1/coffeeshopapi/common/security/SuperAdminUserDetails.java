package org.group1.coffeeshopapi.common.security;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.user.dto.response.SuperAdminResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

/**
 * Represents the config-driven super admin (SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD).
 * Deliberately not backed by a {@code User} row — it exists purely from configuration so it
 * can never be locked out, deleted, or discovered via the database.
 */
public class SuperAdminUserDetails implements UserDetails {

    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final String email;
    private final String passwordHash;

    public SuperAdminUserDetails(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + Role.SUPER_ADMIN.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public SuperAdminResponse toResponse() {
        return SuperAdminResponse.builder()
                .id(ID)
                .fullName("Super Admin")
                .email(email)
                .role(Role.SUPER_ADMIN)
                .status(Status.ACTIVE)
                .build();
    }
}
