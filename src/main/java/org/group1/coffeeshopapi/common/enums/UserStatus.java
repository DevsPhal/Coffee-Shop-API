package org.group1.coffeeshopapi.common.enums;

// Account lifecycle status for a User (Admin/Barista/Customer).
public enum UserStatus {
    // Can log in and use the account normally.
    ACTIVE,
    // Registered but hasn't completed OTP verification yet.
    PENDING_VERIFICATION,
    // Voluntarily or administratively turned off (e.g. a staff member who left) — can be
    // reactivated later.
    DEACTIVATED,
    // Temporarily blocked, typically for a policy violation under review.
    SUSPENDED,
    // Permanently blocked for abuse/violation — stronger than SUSPENDED, not meant to be lifted.
    BANNED,
    // Soft-deleted: the account no longer exists from the user's perspective, but the row (and
    // its order/audit history) is kept rather than hard-deleted.
    DELETED
}
