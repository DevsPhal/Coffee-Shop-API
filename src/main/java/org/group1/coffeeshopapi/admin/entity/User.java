package org.group1.coffeeshopapi.admin.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.group1.coffeeshopapi.common.enums.Role;

@Entity
@Table(name = "auth_users")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 256)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 256)
    private String email;

    @Column(nullable = false, length = 256)
    private String password;

    @Column(nullable = false, columnDefinition = "Text")
    private String familyName;

    @Column(nullable = false, columnDefinition = "Text")
    private String givenName;

    @Column(unique = true)
    private String phoneNumber;

    private String gender;

    private LocalDate dob;

    @Column(length = 256)
    private String profileImage;

    @Column(length = 256)
    private String coverImage;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "loyalty_points", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer loyaltyPoints = 0;

    @Column(name = "notification_preference", length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'IN_APP'")
    @Builder.Default
    private String notificationPreference = "IN_APP";

    @Column(name = "telegram_chat_id", length = 50)
    private String telegramChatId;

    private Boolean accountNonExpired;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean accountNonLocked;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean credentialsNonExpired;

    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        if (fullName == null || fullName.isBlank()) {
            fullName = ((givenName == null ? "" : givenName) + " " + (familyName == null ? "" : familyName)).trim();
        }
        if (givenName == null || givenName.isBlank()) {
            givenName = fullName;
        }
        if (familyName == null) {
            familyName = "";
        }
        if ((username == null || username.isBlank()) && email != null) {
            username = email;
        }
        if (role == null) {
            role = Role.CUSTOMER;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
