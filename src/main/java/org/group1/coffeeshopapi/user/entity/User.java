package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

/**
 * Shared shape for every account. Never persisted on its own — {@code Admin}, {@code Barista},
 * and {@code Customer} each declare the concrete, standalone table ({@code admins},
 * {@code baristas}, {@code customers}) that physically holds these fields (credentials,
 * verification status, Telegram link) alongside their own role-specific columns. One
 * {@link org.group1.coffeeshopapi.user.repository.UserRepository} query still works across all
 * roles (Hibernate unions the three tables), while {@code AdminRepository}/
 * {@code BaristaRepository}/{@code CustomerRepository} let you query a single role directly.
 * <p>
 * {@code status} is the account's full lifecycle state — see {@link UserStatus}. It starts at
 * {@link UserStatus#PENDING_VERIFICATION} until the account completes OTP verification, moves to
 * {@link UserStatus#ACTIVE} afterward (or immediately for staff created by an admin/super admin,
 * who skip that step), and can later be moved to {@link UserStatus#DEACTIVATED},
 * {@link UserStatus#SUSPENDED}, {@link UserStatus#BANNED}, or {@link UserStatus#DELETED} by an
 * admin. Only {@link UserStatus#ACTIVE} accounts can authenticate — see
 * {@link org.group1.coffeeshopapi.common.security.CustomUserDetails#isEnabled()}.
 */
@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Email
    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    private String phoneNumber;

    @Column
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column
    private String telegramChatId;

    public abstract Role getRole();
}