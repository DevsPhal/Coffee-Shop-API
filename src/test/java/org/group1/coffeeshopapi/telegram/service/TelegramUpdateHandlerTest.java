package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.command.TelegramCommand;
import org.group1.coffeeshopapi.telegram.command.TelegramCommandRegistry;
import org.group1.coffeeshopapi.telegram.dto.TelegramChat;
import org.group1.coffeeshopapi.telegram.dto.TelegramContact;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.TelegramUpdate;
import org.group1.coffeeshopapi.telegram.dto.TelegramUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramUpdateHandlerTest {

    private static final Long CHAT_ID = 555L;

    @Mock private TelegramApiClient apiClient;
    @Mock private TelegramLinkService telegramLinkService;

    @Test
    void ignoresAnUpdateWithNoMessage() {
        TelegramUpdateHandler handler = handlerWith();
        handler.handle(new TelegramUpdate(1L, null));
        verifyNoInteractions(apiClient, telegramLinkService);
    }

    @Test
    void ignoresNonCommandText() {
        TelegramUpdateHandler handler = handlerWith();
        handler.handle(new TelegramUpdate(1L, textMessage("just chatting")));
        verifyNoInteractions(apiClient, telegramLinkService);
    }

    @Test
    void repliesWithUnknownCommandMessageWhenNothingMatches() {
        TelegramUpdateHandler handler = handlerWith();
        handler.handle(new TelegramUpdate(1L, textMessage("/nope")));
        verify(apiClient).sendMessage(CHAT_ID, "Unknown command. Send /help to see what I can do.");
    }

    @Test
    void matchesARegisteredCommandRegardlessOfCase() {
        TelegramUpdateHandler handler = handlerWith(fakeCommand("/menu", false, (message, argument) -> "the menu"));

        handler.handle(new TelegramUpdate(1L, textMessage("/Menu")));

        verify(apiClient).sendMessage(CHAT_ID, "the menu");
    }

    @Test
    void sendsAnHtmlReplyForACommandThatOptsIntoHtml() {
        TelegramUpdateHandler handler = handlerWith(fakeCommand("/events", true, (message, argument) -> "<b>events</b>"));

        handler.handle(new TelegramUpdate(1L, textMessage("/events")));

        verify(apiClient).sendHtmlMessage(CHAT_ID, "<b>events</b>");
        verify(apiClient, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void sendsNothingWhenACommandAlreadySentItsOwnReply() {
        TelegramUpdateHandler handler = handlerWith(fakeCommand("/start", true, (message, argument) -> null));

        handler.handle(new TelegramUpdate(1L, textMessage("/start CODE123")));

        verifyNoInteractions(apiClient);
    }

    @Test
    void passesTheArgumentAfterTheCommandNameAndStripsAnyBotMention() {
        List<String> capturedArgument = new ArrayList<>();
        TelegramUpdateHandler handler = handlerWith(fakeCommand("/menu", false, (message, argument) -> {
            capturedArgument.add(argument);
            return "ok";
        }));

        handler.handle(new TelegramUpdate(1L, textMessage("/menu@MyCafeBot Coffee")));

        assertThat(capturedArgument).containsExactly("Coffee");
    }

    @Test
    void routesASharedContactToTelegramLinkServiceInsteadOfCommandDispatch() {
        TelegramUpdateHandler handler = handlerWith();
        TelegramContact contact = new TelegramContact("012345678", 9L, "Test", null);
        TelegramMessage message = new TelegramMessage(1L, new TelegramChat(CHAT_ID, "private"),
                new TelegramUser(9L, "Test", null), null, contact);
        when(telegramLinkService.verifyPendingContact(CHAT_ID, contact, 9L)).thenReturn("verified");

        handler.handle(new TelegramUpdate(1L, message));

        verify(apiClient).sendHtmlMessage(CHAT_ID, "verified");
    }

    @Test
    void fallsBackToAFriendlyMessageInsteadOfLettingAnUnexpectedFailureReachTheWebhookResponse() {
        TelegramUpdateHandler handler = handlerWith(fakeCommand("/menu", false, (message, argument) -> {
            throw new IllegalStateException("boom");
        }));

        handler.handle(new TelegramUpdate(1L, textMessage("/menu")));

        ArgumentCaptor<String> reply = ArgumentCaptor.forClass(String.class);
        verify(apiClient).sendMessage(eq(CHAT_ID), reply.capture());
        assertThat(reply.getValue()).contains("something went wrong");
    }

    private TelegramMessage textMessage(String text) {
        return new TelegramMessage(1L, new TelegramChat(CHAT_ID, "private"), new TelegramUser(9L, "Test", null), text, null);
    }

    private TelegramUpdateHandler handlerWith(TelegramCommand... commands) {
        return new TelegramUpdateHandler(new TelegramCommandRegistry(List.of(commands)), apiClient, telegramLinkService);
    }

    private TelegramCommand fakeCommand(String name, boolean useHtml,
            BiFunction<TelegramMessage, String, String> body) {
        return new TelegramCommand() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String execute(TelegramMessage message, String argument) {
                return body.apply(message, argument);
            }

            @Override
            public boolean useHtml() {
                return useHtml;
            }
        };
    }
}
