package org.group1.coffeeshopapi.common.enums;

// How an account was created/verified: EMAIL covers a customer's own email+OTP
// self-registration, a staff account an admin/super admin created directly (trusted, active
// immediately, no verification — see StaffServiceImpl#create), or a staff/customer account that
// later links Telegram as an alternate login (AuthServiceImpl#loginViaTelegramWidget) without
// changing how it was originally created; TELEGRAM covers an account with no email/password of
// its own at all, created straight from Telegram — either a staff account invited via Telegram
// (StaffServiceImpl#createViaTelegram, verified by sharing a contact whose phone number matches
// the one the admin entered — see TelegramLinkServiceImpl#verifyPendingContact — before it goes
// ACTIVE), or a customer who self-registered through the Telegram Login Widget
// (AuthServiceImpl#registerCustomerViaTelegram, active immediately since Telegram's own signature
// is proof enough). Either way, login from then on is Telegram-widget-only.
public enum RegisterType {
    EMAIL,
    TELEGRAM
}
