package org.group1.coffeeshopapi.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only mirror of every {@link User} row (whichever concrete role table it actually lives
 * in), kept in {@code auth_users} for visibility across all roles in one place. Not part of the
 * {@code User}/{@code Admin}/{@code Barista}/{@code Customer} JPA hierarchy — {@code Admin},
 * {@code Barista}, and {@code Customer} are each their own standalone table (see {@link User}),
 * so this row is written explicitly by {@code AuthUserSyncService} whenever one of those
 * changes, rather than being derived by Hibernate. Its id always matches the source row's id.
 */
@Getter
@Setter
@Entity
@Table(name = "auth_users")
public class AuthUser {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 31)
    private Role role;

    @Column(nullable = false)
    private String fullName;

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
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column
    private String telegramChatId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
