package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.TelegramChat;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.TelegramUser;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.telegram.service.TelegramLoginService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartCommandTest {

    private static final TelegramMessage MESSAGE = new TelegramMessage(1L, new TelegramChat(555L, "private"),
            new TelegramUser(555L, "Alice", "alice"), "/start", null);

    @Mock private TelegramLinkService telegramLinkService;
    @Mock private TelegramLoginService telegramLoginService;
    @InjectMocks private StartCommand command;

    @Test
    void aLoginDeepLinkIsHandedToTheLoginServiceWithoutItsPrefix() {
        when(telegramLoginService.handleStart("AAAAAAAAAAAAAAAAAAAAAA", MESSAGE)).thenReturn(null);

        assertThat(command.execute(MESSAGE, "login_AAAAAAAAAAAAAAAAAAAAAA")).isNull();

        verify(telegramLoginService).handleStart("AAAAAAAAAAAAAAAAAAAAAA", MESSAGE);
        verifyNoInteractions(telegramLinkService);
    }

    @Test
    void anAccountLinkCodeStillGoesToTheLinkService() {
        when(telegramLinkService.resolveLinkCode("ABC234", 555L)).thenReturn("linked");

        assertThat(command.execute(MESSAGE, "ABC234")).isEqualTo("linked");

        verifyNoInteractions(telegramLoginService);
    }
}
