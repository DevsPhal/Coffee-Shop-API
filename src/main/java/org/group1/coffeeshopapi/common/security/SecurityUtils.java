package org.group1.coffeeshopapi.common.security;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.common.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return null;
        }
        return details.user();
    }

    public static Long currentUserId() {
        User user = currentUser();
        return user == null ? null : user.getId();
    }

    public static boolean isCustomer() {
        User user = currentUser();
        return user != null && user.getRole() == Role.CUSTOMER;
    }

    /** Staff roles that can act on behalf of, or view, any customer's resources. */
    public static boolean isStaff() {
        User user = currentUser();
        return user != null && user.getRole() != Role.CUSTOMER;
    }
}