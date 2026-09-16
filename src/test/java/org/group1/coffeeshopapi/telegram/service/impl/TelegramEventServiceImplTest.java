package org.group1.coffeeshopapi.telegram.service.impl;

import org.group1.coffeeshopapi.event.entity.Event;
import org.group1.coffeeshopapi.event.repository.EventRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramEventServiceImplTest {

    private static final Long CHAT_ID = 555L;

    @Mock private EventRepository eventRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private TelegramApiClient apiClient;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @InjectMocks private TelegramEventServiceImpl service;

    @Test
    void sendUpcomingEventsTellsTheChatWhenNothingIsScheduled() {
        when(eventRepository.findByStatusAndEndAtAfterOrderByStartAtAsc(any(), any())).thenReturn(List.of());

        service.sendUpcomingEvents(CHAT_ID);

        verify(apiClient).sendMessage(CHAT_ID, "No upcoming events right now — check back soon! ☕");
        verifyNoInteractions(customerRepository);
    }

    @Test
    void sendUpcomingEventsSendsAPhotoWithCaptionForAnEventWithAFlyerImage() {
        Event event = eventWithoutVenue();
        event.setImageUrl("https://cdn.590stcafe.shop/events/flyer.png");
        when(eventRepository.findByStatusAndEndAtAfterOrderByStartAtAsc(any(), any())).thenReturn(List.of(event));

        service.sendUpcomingEvents(CHAT_ID);

        verify(apiClient).sendHtmlMessage(CHAT_ID, "🎉 <b>Upcoming Events</b>");
        ArgumentCaptor<String> caption = ArgumentCaptor.forClass(String.class);
        verify(apiClient).sendPhoto(eq(CHAT_ID), eq(event.getImageUrl()), caption.capture());
        assertThat(caption.getValue()).contains("Latte Art Night");
        verify(apiClient, never()).sendLocation(any(), any(), any());
    }

    @Test
    void sendUpcomingEventsSendsPlainTextAndDropsAPinForAnEventWithAVenueButNoImage() {
        Event event = eventWithVenue();
        when(eventRepository.findByStatusAndEndAtAfterOrderByStartAtAsc(any(), any())).thenReturn(List.of(event));

        service.sendUpcomingEvents(CHAT_ID);

        verify(apiClient, times(2)).sendHtmlMessage(eq(CHAT_ID), anyString());
        verify(apiClient).sendLocation(CHAT_ID, event.getLatitude(), event.getLongitude());
        verify(apiClient, never()).sendPhoto(any(), any(), any());
    }

    @Test
    void announceNewEventDoesNothingWhenNoCustomerHasALinkedTelegramChat() {
        when(customerRepository.findByTelegramChatIdIsNotNull()).thenReturn(List.of());

        service.announceNewEvent(eventWithoutVenue());

        verifyNoInteractions(apiClient);
    }

    @Test
    void announceNewEventBroadcastsToEveryLinkedCustomer() {
        Customer first = customerWithChat("111");
        Customer second = customerWithChat("222");
        when(customerRepository.findByTelegramChatIdIsNotNull()).thenReturn(List.of(first, second));

        service.announceNewEvent(eventWithoutVenue());

        for (long chatId : List.of(111L, 222L)) {
            verify(apiClient).sendHtmlMessage(chatId, "📢 <b>New Event!</b>");
            ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
            verify(apiClient, times(2)).sendHtmlMessage(eq(chatId), body.capture());
            assertThat(body.getAllValues().get(1)).contains("Send /events to see what else is coming up.");
        }
    }

    @Test
    void sendStartingSoonRemindersSendsOnlyOnceForANewlyDueEvent() {
        Event event = eventWithoutVenue();
        when(eventRepository.findByStatusAndStartAtBetween(any(), any(), any())).thenReturn(List.of(event));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(customerRepository.findByTelegramChatIdIsNotNull()).thenReturn(List.of(customerWithChat("333")));

        service.sendStartingSoonReminders();

        verify(apiClient).sendHtmlMessage(333L, "⏰ <b>Starting Soon!</b>");
    }

    @Test
    void sendStartingSoonRemindersSkipsAnEventAlreadyRemindedAndNeverLoadsCustomers() {
        Event event = eventWithoutVenue();
        when(eventRepository.findByStatusAndStartAtBetween(any(), any(), any())).thenReturn(List.of(event));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        service.sendStartingSoonReminders();

        verifyNoInteractions(customerRepository, apiClient);
    }

    private Event eventWithoutVenue() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Latte Art Night");
        event.setStartAt(LocalDateTime.now().plusHours(1));
        event.setEndAt(LocalDateTime.now().plusHours(3));
        return event;
    }

    private Event eventWithVenue() {
        Event event = eventWithoutVenue();
        event.setLatitude(new BigDecimal("11.556800"));
        event.setLongitude(new BigDecimal("104.928200"));
        return event;
    }

    private Customer customerWithChat(String chatId) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTelegramChatId(chatId);
        return customer;
    }
}
