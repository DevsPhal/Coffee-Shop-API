package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.user.entity.User;

public interface TelegramProfileSyncService {

    /**
     * Brings a Telegram-linked account's username and profile photo in line with Telegram. The photo is copied into
     * our own storage, and a photo the user or an admin uploaded by hand is never replaced. Best effort: a failure
     * leaves the account as it was and never stops the login or link that triggered it.
     *
     * @param telegramUsername the username Telegram just reported, or null to leave it alone
     * @param widgetPhotoUrl   the Login Widget's signed photo_url, used when the Bot API can't supply the photo; may be null
     */
    void sync(User user, Long telegramUserId, String telegramUsername, String widgetPhotoUrl);
}
