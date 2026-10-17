package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.TelegramCallbackQuery;
import org.group1.coffeeshopapi.telegram.dto.TelegramLoginChallenge;
import org.group1.coffeeshopapi.telegram.dto.TelegramLoginOutcome;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;

public interface TelegramLoginService {
    String START_PREFIX = "login_";
    String CALLBACK_PREFIX = "tglogin:";

    TelegramLoginChallenge createChallenge();

    TelegramLoginOutcome checkChallenge(String requestId, String pollToken);

    String handleStart(String requestId, TelegramMessage message);

    void handleCallback(TelegramCallbackQuery callbackQuery);
}
