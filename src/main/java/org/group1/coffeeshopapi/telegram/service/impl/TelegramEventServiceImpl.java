package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.constant.RedisKeys;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.event.entity.Event;
import org.group1.coffeeshopapi.event.repository.EventRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramEventService;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelegramEventServiceImpl implements TelegramEventService {

    private static final DateTimeFormatter EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    // How long a "reminder already sent" mark sticks around — just needs to outlive the 24h
    // lookahead window.
    private static final Duration REMINDER_MARK_TTL = Duration.ofDays(7);

    private final EventRepository eventRepository;
    private final CustomerRepository customerRepository;
    private final TelegramApiClient apiClient;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void sendUpcomingEvents(Long chatId) {
        List<Event> events = eventRepository.findByStatusAndEndAtAfterOrderByStartAtAsc(Status.ACTIVE, LocalDateTime.now());
        if (events.isEmpty()) {
            apiClient.sendMessageWithButtons(chatId, "🎉 No upcoming events right now — check back soon! ☕");
            return;
        }

        apiClient.sendHtmlMessage(chatId, "🎉 <b>Upcoming Events</b>");
        for (Event event : events) {
            sendEvent(chatId, event, null);
        }
        // Brings back the quick-action keyboard after the event list.
        apiClient.sendMessageWithButtons(chatId, "Use the buttons below to keep exploring 👇");
    }

    // REQUIRES_NEW, not the class-level readOnly transaction: this is called from inside
    // EventServiceImpl.create()'s own transaction, and joining that one would mean any failure
    // here (bad data, a broadcast error) marks the caller's transaction rollback-only even though
    // the caller catches the exception — the event would silently fail to save. A separate
    // transaction keeps this side effect from ever affecting whether the event gets created.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void announceNewEvent(Event event) {
        List<Customer> recipients = customerRepository.findByTelegramChatIdIsNotNull();
        if (recipients.isEmpty()) {
            return;
        }

        String header = "📢 <b>New Event!</b>\n\n";
        String footer = "\nSend /events to see what else is coming up.";
        broadcast(recipients, event, header, footer);
    }

    @Override
    public void sendStartingSoonReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> startingSoon = eventRepository.findByStatusAndStartAtBetween(Status.ACTIVE, now, now.plusHours(24));
        if (startingSoon.isEmpty()) {
            return;
        }

        List<Customer> recipients = null;
        for (Event event : startingSoon) {
            String key = RedisKeys.TELEGRAM_EVENT_REMINDER_PREFIX + event.getId();
            boolean firstReminder = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(key, "1", REMINDER_MARK_TTL));
            if (!firstReminder) {
                continue;
            }

            // Fetched lazily so an empty lookahead window skips the customer query entirely.
            if (recipients == null) {
                recipients = customerRepository.findByTelegramChatIdIsNotNull();
            }
            if (recipients.isEmpty()) {
                continue;
            }

            broadcast(recipients, event, "⏰ <b>Starting Soon!</b>\n\n", "\nDon't miss it!");
        }
    }

    // Sends one event: a photo with caption if it has a flyer, plain text otherwise, then a map
    // pin if it has a location.
    private void sendEvent(Long chatId, Event event, String footer) {
        StringBuilder sb = new StringBuilder();
        appendEventBlock(sb, event);
        if (footer != null) {
            sb.append(footer);
        }
        String text = sb.toString();

        if (event.getImageUrl() != null) {
            apiClient.sendPhoto(chatId, event.getImageUrl(), text);
        } else {
            apiClient.sendHtmlMessage(chatId, text);
        }
        if (event.getLatitude() != null && event.getLongitude() != null) {
            apiClient.sendLocation(chatId, event.getLatitude(), event.getLongitude());
        }
    }

    private void appendEventBlock(StringBuilder sb, Event event) {
        sb.append("<b>").append(TelegramFormat.escape(TelegramFormat.titleCase(event.getTitle()))).append("</b>\n");
        sb.append("🗓 ").append(event.getStartAt().format(EVENT_DATE_FORMAT))
                .append(" – ").append(event.getEndAt().format(EVENT_DATE_FORMAT)).append('\n');
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            sb.append(TelegramFormat.escape(TelegramFormat.professionalize(event.getDescription()))).append('\n');
        }
    }

    private void broadcast(List<Customer> recipients, Event event, String header, String footer) {
        for (Customer customer : recipients) {
            try {
                Long chatId = Long.parseLong(customer.getTelegramChatId());
                apiClient.sendHtmlMessage(chatId, header.stripTrailing());
                sendEvent(chatId, event, footer);
            } catch (Exception ex) {
                log.error("Failed to send event {} to customer {}", event.getId(), customer.getId(), ex);
            }
        }
    }
}
