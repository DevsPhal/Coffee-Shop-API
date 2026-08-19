package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;

/**
 * Shared identity for every account, stored in {@code auth_users}. Holds the fields that matter
 * for authentication (credentials, verification status) regardless of role. Each role's
 * additional data lives in its own joined table (see {@link Role}'s subclasses:
 * {@code Admin}, {@code Barista}, {@code Customer}) — Hibernate joins them automatically, so
 * one {@link org.group1.coffeeshopapi.user.repository.UserRepository} query works across all
 * roles while {@code AdminRepository}/{@code BaristaRepository}/{@code CustomerRepository} let
 * you query a single role directly.
 * <p>
 * {@code status} is the verification gate: {@link Status#INACTIVE} until the account completes
 * OTP verification, {@link Status#ACTIVE} afterward (or immediately for staff created by an
 * admin/super admin, who skip that step).
 */
@Getter
@Setter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@Table(name = "auth_users")
public abstract class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Email
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public abstract Role getRole();
}