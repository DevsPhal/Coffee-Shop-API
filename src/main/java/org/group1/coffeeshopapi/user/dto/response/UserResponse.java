package org.group1.coffeeshopapi.user.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.group1.coffeeshopapi.common.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private Role role;
    private String username;
    private String email;
    private String familyName;
    private String givenName;
    private String phoneNumber;
    private String gender;
    private LocalDate dob;
    private String profileImage;
    private String coverImage;
    private String address;
    private Integer loyaltyPoints;
    private String notificationPreference;
    private String telegramChatId;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
