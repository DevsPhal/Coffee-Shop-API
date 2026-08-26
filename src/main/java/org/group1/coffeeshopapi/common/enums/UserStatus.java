package org.group1.coffeeshopapi.common.enums;

// Account lifecycle status for a User row (Admin/Barista/Customer) — kept separate from the
// catalog-facing Status (Product/Category/Event/...), since "banned"/"deleted" have no meaning
// there. See User.status and CustomUserDetails.isEnabled() for how this gates login.
public enum UserStatus {
    // Can log in and use the account normally.
    ACTIVE,
    // Registered but hasn't completed OTP verification yet — see AuthServiceImpl.register.
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
