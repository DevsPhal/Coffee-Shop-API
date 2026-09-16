package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.TelegramChat;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventsCommandTest {

    @Mock private TelegramEventService telegramEventService;
    @InjectMocks private EventsCommand command;

    @Test
    void isRegisteredUnderTheEventsSlashCommand() {
        assertThat(command.name()).isEqualTo("/events");
    }

    @Test
    void delegatesToTelegramEventServiceAndSendsNoReplyOfItsOwn() {
        TelegramMessage message = new TelegramMessage(1L, new TelegramChat(555L, "private"), null, "/events", null);

        String reply = command.execute(message, null);

        verify(telegramEventService).sendUpcomingEvents(555L);
        assertThat(reply).isNull();
    }
}
