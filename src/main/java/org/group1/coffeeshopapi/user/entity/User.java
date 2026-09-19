package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import org.group1.coffeeshopapi.common.enums.GenderConverter;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

// Shared shape for every account. Never persisted on its own — Admin, Barista, and Customer
// each have their own table for these fields plus their own role-specific columns.
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

    // A converter, not @Enumerated(EnumType.STRING) — a blank/unrecognized stored value must
    // become null on read, not throw and crash the whole query. See GenderConverter.
    @Convert(converter = GenderConverter.class)
    @Column(length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column
    private String telegramChatId;

    // Telegram's own @handle, captured once at Telegram widget registration time — not kept in
    // sync if the person changes it in Telegram afterward.
    @Column
    private String telegramUsername;

    // How this account was created/verified — set once and never changed afterward, even if a
    // Telegram chat is linked/unlinked later.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegisterType registerType;

    public abstract Role getRole();
}