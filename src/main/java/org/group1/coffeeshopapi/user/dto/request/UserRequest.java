package org.group1.coffeeshopapi.user.dto.request;

import java.time.LocalDate;

import org.group1.coffeeshopapi.common.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String fullName;
    private Role role;

    @Size(min = 3, max = 64)
    private String username;

    @Email
    private String email;

    @Size(min = 6)
    private String password;

    private String familyName;
    private String givenName;
    private String phoneNumber;
    private String gender;
    private LocalDate dob;
    private String profileImage;
    private String coverImage;
    private String address;
    private String notificationPreference;
    private String telegramChatId;
    private Boolean accountNonLocked;
    private Boolean enabled;
}
