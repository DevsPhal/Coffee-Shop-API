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
import org.group1.coffeeshopapi.common.enums.Status;

/**
 * Shared shape for every account. Never persisted on its own — {@code Admin}, {@code Barista},
 * and {@code Customer} each declare the concrete, standalone table ({@code admins},
 * {@code baristas}, {@code customers}) that physically holds these fields (credentials,
 * verification status, Telegram link) alongside their own role-specific columns. One
 * {@link org.group1.coffeeshopapi.user.repository.UserRepository} query still works across all
 * roles (Hibernate unions the three tables), while {@code AdminRepository}/
 * {@code BaristaRepository}/{@code CustomerRepository} let you query a single role directly.
 * <p>
 * {@code status} is the verification gate: {@link Status#INACTIVE} until the account completes
 * OTP verification, {@link Status#ACTIVE} afterward (or immediately for staff created by an
 * admin/super admin, who skip that step).
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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column
    private String telegramChatId;

    public abstract Role getRole();
}